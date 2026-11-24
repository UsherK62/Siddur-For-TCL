package com.pureflip.siddur;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.View;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/**
 * Not a pass/fail test: measures host-JVM timings and allocations (Robolectric, native graphics) and writes them
 * to the file in system property "perf.out" (default build/perf/latest.txt). Absolute numbers are a
 * desktop JVM, not an MT6739; compare before / after only.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PerfProbeTest {
    private Context ctx;
    private Prefs prefs;
    private final StringBuilder out = new StringBuilder();

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
    }

    private static void settle() {
        for (int i = 0; i < 6; i++) {
            ShadowLooper.idleMainLooper();
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
    }

    private static long alloc() {
        try { // java.lang.management isn't on the Android compile classpath: reflect
            Object mx = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);
            java.lang.reflect.Method m = Class.forName("com.sun.management.ThreadMXBean").getMethod("getThreadAllocatedBytes", long.class);
            return (Long) m.invoke(mx, Thread.currentThread().getId());
        } catch (Exception e) {
            return 0;
        }
    }

    private void line(String name, double v, String unit) {
        out.append(String.format(Locale.US, "%-48s %10.3f %s%n", name, v, unit));
    }

    private static void clearDocCache() throws Exception {
        Field f = Doc.class.getDeclaredField("CACHE");
        f.setAccessible(true);
        ((android.util.LruCache<?, ?>) f.get(null)).evictAll();
    }

    private <T extends Activity> double launchMs(Class<T> cls, Intent i, int reps) {
        long best = Long.MAX_VALUE;
        for (int r = 0; r < reps; r++) {
            long t = System.nanoTime();
            Robolectric.buildActivity(cls, i).setup();
            long d = System.nanoTime() - t;
            best = Math.min(best, d);
            settle();
        }
        return best / 1e6;
    }

    private double drawMs(Activity a, int reps, long[] allocOut) {
        View root = a.getWindow().getDecorView();
        Bitmap b = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        root.draw(c); // warm
        long a0 = alloc();
        long t = System.nanoTime();
        for (int i = 0; i < reps; i++) {
            root.invalidate();
            root.draw(c);
        }
        long d = System.nanoTime() - t;
        allocOut[0] = (alloc() - a0) / reps;
        return d / 1e6 / reps;
    }

    @Test
    public void probe() throws Exception {
        // font loading on the UI thread at first screen (cold: static Typefaces cleared)
        long fb = Long.MAX_VALUE;
        for (int r = 0; r < 5; r++) {
            for (Field f : Ui.class.getDeclaredFields()) {
                if (f.getType() == android.graphics.Typeface.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    f.set(null, null);
                }
                if (f.getName().equals("settingsFonts")) {
                    f.setAccessible(true);
                    f.set(null, false);
                }
            }
            long t0 = System.nanoTime();
            Ui.fonts(ctx);
            fb = Math.min(fb, System.nanoTime() - t0);
        }
        line("Ui.fonts on first screen (UI thread, best of 5)", fb / 1e6, "ms");
        try {
            Field sf = Ui.class.getDeclaredField("settingsFonts");
            sf.setAccessible(true);
            sf.set(null, false);
            long t0 = System.nanoTime();
            Ui.face("en", 700);
            line("  Inter+Heebo (now lazy / preloaded off-UI)", (System.nanoTime() - t0) / 1e6, "ms");
        } catch (NoSuchFieldException e) {
            line("  Inter+Heebo (inside Ui.fonts)", 0, "ms");
        }
        // text parse (cold, no cache)
        String[] docs = {"shacharis", "mincha", "maariv", "bentching", "tehillim"};
        for (String d : docs) {
            Doc.loadSync(ctx, "ashkenaz", d);
            long best = Long.MAX_VALUE;
            for (int r = 0; r < 5; r++) {
                clearDocCache();
                long t = System.nanoTime();
                Doc.loadSync(ctx, "ashkenaz", d);
                best = Math.min(best, System.nanoTime() - t);
            }
            line("doc parse " + d + " (best of 5)", best / 1e6, "ms");
        }
        // same-JVM A/B of the paragraph markup parser: Html.fromHtml (old) vs the fast parser, all shacharis
        java.util.List<String> paras = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                ctx.getAssets().open(Doc.path("ashkenaz", "shacharis")), "UTF-8"))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] p = ln.split("\t", -1);
                if (p.length >= 3 && p[0].equals("P")) {
                    paras.add(p[2]);
                }
            }
        }
        long slow = Long.MAX_VALUE;
        long fast = Long.MAX_VALUE;
        for (int r = 0; r < 12; r++) {
            long t0 = System.nanoTime();
            for (String x : paras) {
                Doc.slowHtml(x);
            }
            slow = Math.min(slow, System.nanoTime() - t0);
            t0 = System.nanoTime();
            for (String x : paras) {
                Doc.html(x);
            }
            fast = Math.min(fast, System.nanoTime() - t0);
        }
        line("markup, " + paras.size() + " shacharis paragraphs: Html.fromHtml", slow / 1e6, "ms");
        line("markup, same paragraphs: fast parser", fast / 1e6, "ms");
        // zmanim
        Luach.current(prefs).nowTefillah();
        long t = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            Luach.current(prefs).nowTefillah();
        }
        line("Luach.current+nowTefillah (avg of 20)", (System.nanoTime() - t) / 1e6 / 20, "ms");

        // screen launches (best of 3, host JVM)
        for (String layout : Prefs.LAYOUTS) {
            prefs.layout(layout);
            line("launch Home/" + layout, launchMs(HomeActivity.class, new Intent(ctx, HomeActivity.class), 3), "ms");
        }
        line("launch Settings", launchMs(SettingsActivity.class, new Intent(ctx, SettingsActivity.class), 3), "ms");
        Doc.loadSync(ctx, "ashkenaz", "shacharis");
        line("launch Reader/shacharis (text cached)", launchMs(ReaderActivity.class, new Intent(ctx, ReaderActivity.class).putExtra("doc", "shacharis"), 3), "ms");

        // Settings: draw cost + allocation per frame while a row is focused
        SettingsActivity s = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        settle();
        long[] al = new long[1];
        line("Settings frame draw (avg of 200)", drawMs(s, 200, al), "ms");
        line("Settings alloc per frame", al[0], "bytes");
        // Settings: key handling + layout per DPAD_DOWN (32 presses)
        long a0 = alloc();
        t = System.nanoTime();
        for (int i = 0; i < 32; i++) {
            s.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0));
            s.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0));
            settle();
        }
        line("Settings DPAD_DOWN press incl. animation (avg 32)", (System.nanoTime() - t) / 1e6 / 32, "ms");
        line("Settings alloc per press", (alloc() - a0) / 32.0, "bytes");
        // city popup open
        while (s.rows.indexOf(s.getWindow().getDecorView().findFocus()) != 4) {
            s.focusRow(4);
            settle();
        }
        t = System.nanoTime();
        s.rows.get(4).performClick();
        settle();
        line("Settings city popup open", (System.nanoTime() - t) / 1e6, "ms");
        line("Settings frame with city popup", drawMs(s, 100, al), "ms");
        line("  alloc per frame (popup)", al[0], "bytes");

        // Reader: held D-pad (key repeat), 120 events
        ReaderActivity r = Robolectric.buildActivity(ReaderActivity.class, new Intent(ctx, ReaderActivity.class).putExtra("doc", "shacharis")).setup().get();
        settle();
        line("Reader frame draw (avg of 100)", drawMs(r, 100, al), "ms");
        line("Reader alloc per frame", al[0], "bytes");
        a0 = alloc();
        t = System.nanoTime();
        for (int i = 0; i < 120; i++) {
            r.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, i));
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS); // ~20 repeats / s
        }
        r.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0));
        settle();
        line("Reader held DPAD_DOWN, per repeat (120)", (System.nanoTime() - t) / 1e6 / 120, "ms");
        line("Reader alloc per repeat", (alloc() - a0) / 120.0, "bytes");

        File f = new File(System.getProperty("perf.out", "build/perf/latest.txt"));
        f.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(f)) {
            w.write(out.toString());
        }
        System.out.println(out);
    }
}
