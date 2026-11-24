package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowLooper;

/**
 * Smoothness and crash paths for a weak D-pad phone: held keys (key repeat), popups opened / closed in
 * the middle of their animations, recreate with a popup open, damaged prefs, missing text, random key mashing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class SmoothnessTest {
    private static final long REPEAT_MS = 50L; // typical feature-phone key repeat (~20 / s)
    private static final long FRAME_MS = 16L;
    private Context ctx;
    private Prefs prefs;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
        // Real 16 ms frames: by default Robolectric runs a whole animation inside one idle() call,
        // which would hide exactly the per-frame jumps this test looks for.
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_MS));
    }

    @After
    public void tearDown() {
        ShadowChoreographer.setPaused(false);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(1));
    }

    /** Lets every animation (all < 300 ms) and layout pass finish, frame by frame. */
    private static void settle() {
        for (int i = 0; i < 45; i++) { // 720 ms in 16 ms frames
            ShadowLooper.idleMainLooper(FRAME_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static void frames(long ms) {
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS);
    }

    private static KeyEvent down(int code, int repeat) {
        return new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, repeat);
    }

    private static KeyEvent up(int code) {
        return new KeyEvent(0, 0, KeyEvent.ACTION_UP, code, 0);
    }

    private static <T> T field(Object o, String name) throws Exception {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                T v = (T) f.get(o);
                return v;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private ReaderActivity reader(String doc) {
        ReaderActivity r = Robolectric.buildActivity(ReaderActivity.class,
                new Intent(ctx, ReaderActivity.class).putExtra("doc", doc)).setup().get();
        for (int i = 0; i < 100 && Doc.cached(prefs.nusach(), doc) == null; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
            ShadowLooper.idleMainLooper();
        }
        settle();
        return r;
    }

    /** Absolute scroll of a ListView, from a child that is on screen in both frames. */
    private static final class ListTracker {
        final ListView l;
        int[] tops = new int[0];
        int first;

        ListTracker(ListView l) {
            this.l = l;
            snap();
        }

        void snap() {
            first = l.getFirstVisiblePosition();
            tops = new int[l.getChildCount()];
            for (int i = 0; i < tops.length; i++) {
                tops[i] = l.getChildAt(i).getTop();
            }
        }

        /** px scrolled down since the last snap (negative = up); Integer.MIN_VALUE when no common child. */
        int delta() {
            int f2 = l.getFirstVisiblePosition();
            int d = Integer.MIN_VALUE;
            for (int i = 0; i < l.getChildCount(); i++) {
                int pos = f2 + i;
                int j = pos - first;
                if (j >= 0 && j < tops.length) {
                    d = tops[j] - l.getChildAt(i).getTop();
                    break;
                }
            }
            snap();
            return d;
        }
    }

    // ------------------------------------------------------------------ key repeat

    @Test
    public void readerHeldDownScrollsSteadilyWithoutJumpsOrReversals() throws Exception {
        ReaderActivity r = reader("shacharis");
        ListView list = field(r, "list");
        ListTracker t = new ListTracker(list);
        int h = list.getHeight();
        assertTrue(h > 0);
        int maxFrame = 0;
        int total = 0;
        int frames = 0;
        int stalls = 0;
        StringBuilder trace = new StringBuilder();
        int prevD = 0;
        long jerkSum = 0;
        int jerkMax = 0;
        int jerkN = 0;
        // hold DPAD_DOWN for 6 s: a repeat every 50 ms, frames every 16 ms in between
        long now = 0;
        long nextKey = 0;
        int repeat = 0;
        while (now < 6000) {
            if (now >= nextKey) {
                r.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_DOWN, repeat++));
                nextKey += REPEAT_MS;
            }
            frames(FRAME_MS);
            now += FRAME_MS;
            int d = t.delta();
            assertTrue("no common child between frames: the list jumped (frame " + frames + ")", d != Integer.MIN_VALUE);
            assertTrue("scrolled backwards while holding DOWN: " + d, d >= 0);
            maxFrame = Math.max(maxFrame, d);
            total += d;
            if (frames < 60) {
                trace.append(d).append(now >= nextKey - REPEAT_MS && now - FRAME_MS < nextKey - REPEAT_MS ? "k " : " ");
            }
            if (now > 1000 && d == 0) {
                stalls++;
            }
            if (now > 1000) {
                int j = Math.abs(d - prevD);
                jerkSum += j;
                jerkMax = Math.max(jerkMax, j);
                jerkN++;
            }
            prevD = d;
            frames++;
        }
        r.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_DOWN));
        settle();
        System.out.println("reader hold: frames=" + frames + " total=" + total + "px maxFrame=" + maxFrame
                + "px (list " + h + "px) stalls=" + stalls + " speedChange mean=" + String.format(java.util.Locale.US, "%.1f", jerkSum / (double) jerkN) + "px max=" + jerkMax + "px" + "\n  first frames: " + trace);
        assertTrue("did not move", total > h * 4);
        // A frame never moves more than a third of the screen: no end-of-glide snaps.
        assertTrue("jump of " + maxFrame + "px in one 16 ms frame", maxFrame <= h / 3);
        // Speed changes little from frame to frame (old end()-and-restart glide: max 18 px at this size).
        assertTrue("speed jumps by " + jerkMax + "px between frames", jerkMax <= h / 12);
        // focus line is on a paragraph that is on screen
        int cur = field(r, "cur");
        int at = cur - list.getFirstVisiblePosition();
        assertTrue(at >= 0 && at < list.getChildCount());
        View v = list.getChildAt(at);
        assertTrue(v.getBottom() > 0 && v.getTop() < list.getHeight());
    }

    @Test
    public void readerHeldUpAfterDownComesBackToTheStart() throws Exception {
        ReaderActivity r = reader("mincha");
        ListView list = field(r, "list");
        for (int i = 0; i < 40; i++) {
            r.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_DOWN, i));
            frames(REPEAT_MS);
        }
        r.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_DOWN));
        settle();
        assertTrue(list.getFirstVisiblePosition() > 0);
        ListTracker t = new ListTracker(list);
        for (int i = 0; i < 400 && (list.getFirstVisiblePosition() > 0 || (int) field(r, "cur") > 0); i++) {
            r.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_UP, i));
            for (int k = 0; k < 3; k++) {
                frames(FRAME_MS);
                int d = t.delta();
                assertTrue(d != Integer.MIN_VALUE && d <= 0 && -d <= list.getHeight() / 3);
            }
        }
        r.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_UP));
        settle();
        assertEquals(0, (int) field(r, "cur"));
        assertEquals(0, list.getFirstVisiblePosition());
    }

    @Test
    public void settingsHeldDownKeepsFocusedRowInViewAndScrollsOneWay() throws Exception {
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        settle();
        SmoothScroll s = field(a, "scroll");
        int last = s.getScrollY();
        int n = a.rows.size();
        for (int i = 0; i < n + 6; i++) {
            a.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_DOWN, i));
            for (long t = 0; t < REPEAT_MS; t += FRAME_MS) {
                frames(FRAME_MS);
                int y = s.getScrollY();
                assertTrue("scroll went back up while holding DOWN", y >= last);
                assertTrue("scroll jumped " + (y - last), y - last <= s.getHeight() / 3);
                last = y;
            }
        }
        a.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_DOWN));
        settle();
        // held repeat stops at the last row (no wrap on repeat)
        View f = a.getWindow().getDecorView().findFocus();
        assertEquals(n - 1, a.rows.indexOf(f));
        assertFullyVisible(s, f);
        // and every row on the way back up ends fully visible
        for (int i = n - 1; i > 0; i--) {
            a.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_UP, 0));
            a.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_UP));
            settle();
            assertFullyVisible(s, a.getWindow().getDecorView().findFocus());
        }
    }

    private static void assertFullyVisible(SmoothScroll s, View row) {
        Rect r = new Rect(0, 0, row.getWidth(), row.getHeight());
        ((ViewGroup) s.getChildAt(0)).offsetDescendantRectToMyCoords(row, r);
        assertTrue("row top " + r.top + " above view " + s.getScrollY(), r.top >= s.getScrollY());
        assertTrue("row bottom " + r.bottom + " below view", r.bottom <= s.getScrollY() + s.getHeight());
    }

    @Test
    public void cityPopupHeldDownReachesTheEndAndStaysInView() {
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        settle();
        a.focusRow(4);
        settle();
        a.rows.get(4).performClick();
        settle();
        ChoicePopup p = a.choice();
        assertTrue(p.isShowing());
        for (int i = 0; i < p.rows.size() + 5; i++) {
            a.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_DOWN, i + 1));
            frames(REPEAT_MS);
        }
        a.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_DOWN));
        settle();
        View f = a.getWindow().getDecorView().findFocus();
        assertEquals(p.rows.size() - 1, p.rows.indexOf(f));
        Rect vis = new Rect();
        assertTrue(f.getGlobalVisibleRect(vis));
        assertEquals(f.getHeight(), vis.height());
    }

    // ------------------------------------------------------------------ popups mid-animation

    @Test
    public void backDuringPopupOpenAnimationThenNewPopupKeepsCancelLabelAndFocus() {
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        settle();
        a.focusRow(4);
        settle();
        a.rows.get(4).performClick();
        frames(40); // in the middle of the 150 ms open
        a.dispatchKeyEvent(down(KeyEvent.KEYCODE_BACK, 0));
        a.dispatchKeyEvent(up(KeyEvent.KEYCODE_BACK));
        assertFalse(a.choice().isShowing());
        frames(30); // old popup still fading out (120 ms)...
        a.focusRow(1);
        a.rows.get(1).performClick(); // ...when the theme popup opens
        ChoicePopup second = a.choice();
        settle(); // the old popup's close runs now; it must not undo the new one
        assertTrue(second.isShowing());
        assertEquals(Str.get(a.lang, "cancel"), a.barRight.getText().toString());
        View f = a.getWindow().getDecorView().findFocus();
        assertTrue("focus left the open popup", second.rows.contains(f));
        a.dispatchKeyEvent(down(KeyEvent.KEYCODE_BACK, 0));
        a.dispatchKeyEvent(up(KeyEvent.KEYCODE_BACK));
        settle();
        assertEquals(Str.get(a.lang, "back"), a.barRight.getText().toString());
        assertEquals(1, a.rows.indexOf(a.getWindow().getDecorView().findFocus()));
    }

    @Test
    public void rapidDoubleOkInReaderMenuRunsTheActionOnce() throws Exception {
        ReaderActivity r = reader("maariv");
        r.dispatchKeyEvent(down(KeyEvent.KEYCODE_MENU, 0));
        r.dispatchKeyEvent(up(KeyEvent.KEYCODE_MENU));
        settle();
        Popup p = r.popup;
        assertNotNull(p);
        assertTrue(p.isShowing());
        assertEquals(Str.get(r.lang, "cancel"), r.barRight.getText().toString());
        r.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_DOWN, 0)); // "add bookmark"
        r.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_DOWN));
        settle();
        int before = prefs.bookmarks().size();
        for (int i = 0; i < 2; i++) { // two OK presses 30 ms apart, inside the close animation
            r.dispatchKeyEvent(down(KeyEvent.KEYCODE_DPAD_CENTER, 0));
            r.dispatchKeyEvent(up(KeyEvent.KEYCODE_DPAD_CENTER));
            frames(30);
        }
        settle();
        assertEquals(before + 1, prefs.bookmarks().size());
        assertFalse(p.isShowing());
        assertFalse(Str.get(r.lang, "cancel").equals(r.barRight.getText().toString()));
    }

    @Test
    public void recreateWithPopupsOpenDoesNotCrash() throws Exception {
        // Robolectric's ActivityController.recreate() needs the default (unpaused) choreographer.
        ShadowChoreographer.setPaused(false);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(1));
        ActivityController<SettingsActivity> c = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup();
        settle();
        c.get().focusRow(4);
        settle();
        c.get().rows.get(4).performClick();
        frames(60);
        c.recreate();
        settle();
        assertNotNull(c.get().getWindow().getDecorView().findFocus());
        // theme picked, then recreate while the pick's close animation runs
        SettingsActivity a = c.get();
        a.focusRow(1);
        settle();
        a.rows.get(1).performClick();
        settle();
        a.choice().pick(3);
        frames(100);
        c.recreate();
        settle();
        assertEquals(Pal.ALL[3].id, prefs.theme());

        ActivityController<ReaderActivity> rc = Robolectric.buildActivity(ReaderActivity.class,
                new Intent(ctx, ReaderActivity.class).putExtra("doc", "bentching")).setup();
        settle();
        reader("bentching");
        rc.get().dispatchKeyEvent(down(KeyEvent.KEYCODE_MENU, 0));
        rc.get().dispatchKeyEvent(up(KeyEvent.KEYCODE_MENU));
        frames(50);
        rc.recreate();
        settle();
        assertFalse(rc.get().isFinishing());
    }

    // ------------------------------------------------------------------ damaged state

    @Test
    public void wrongTypedOrUnknownPrefsFallBackToDefaults() {
        ctx.getSharedPreferences("siddur", 0).edit().clear()
                .putBoolean("setupDone", true)
                .putInt("theme", 7).putInt("lang", 1).putBoolean("layout", true)
                .putString("nusach", "bogus").putString("size", "xxl").putInt("city", 3)
                .putString("israel", "yes").putString("gen", "x").putInt("bookmarks", 2)
                .commit();
        Prefs p = new Prefs(ctx);
        assertEquals("forest", p.theme());
        assertEquals("en", p.lang());
        assertEquals("list", p.layout());
        assertEquals("ashkenaz", p.nusach());
        assertEquals("m", p.size());
        assertEquals("brooklyn", p.city());
        assertFalse(p.israel());
        assertEquals(0, p.styleGen());
        assertTrue(p.bookmarks().isEmpty());
        for (Class<? extends Activity> cls : new Class[] {HomeActivity.class, SettingsActivity.class, ListActivity.class, SetupActivity.class}) {
            Robolectric.buildActivity(cls, new Intent(ctx, cls)).setup();
            settle();
        }
        reader("shacharis");
    }

    @Test
    public void missingTextClosesTheReaderInsteadOfHanging() throws Exception {
        ActivityController<ReaderActivity> c = Robolectric.buildActivity(ReaderActivity.class,
                new Intent(ctx, ReaderActivity.class).putExtra("doc", "no_such_text"));
        c.setup();
        // keys before the text arrives must be harmless
        for (int code : new int[] {KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_2}) {
            c.get().dispatchKeyEvent(down(code, 0));
            c.get().dispatchKeyEvent(up(code));
        }
        for (int i = 0; i < 50 && !c.get().isFinishing(); i++) {
            Thread.sleep(20);
            settle();
        }
        assertTrue(c.get().isFinishing());
        // the text thread survived: a real text still loads afterwards
        reader("mincha");
        assertNotNull(Doc.cached(prefs.nusach(), "mincha"));
    }

    // ------------------------------------------------------------------ key mashing

    private static final int[] KEYS = {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
        KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND, KeyEvent.KEYCODE_SOFT_LEFT,
        KeyEvent.KEYCODE_SOFT_RIGHT,
    };

    private void mash(Activity a, Random rnd, int n) {
        for (int i = 0; i < n; i++) {
            int code = KEYS[rnd.nextInt(KEYS.length)];
            int reps = rnd.nextInt(4) == 0 ? 1 + rnd.nextInt(6) : 1; // sometimes held
            for (int k = 0; k < reps; k++) {
                a.dispatchKeyEvent(down(code, k));
                frames(rnd.nextInt(3) == 0 ? 0 : 1 + rnd.nextInt(REPEAT_MS > 0 ? 60 : 1));
            }
            a.dispatchKeyEvent(up(code));
            if (rnd.nextInt(5) == 0) {
                frames(200);
            }
        }
        settle();
    }

    @Test
    public void randomKeyMashingOnEveryScreenDoesNotCrash() {
        // Robolectric's ActivityController.recreate() needs the default (unpaused) choreographer.
        ShadowChoreographer.setPaused(false);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(1));
        Random rnd = new Random(6739);
        for (String layout : Prefs.LAYOUTS) {
            prefs.layout(layout);
            mash(Robolectric.buildActivity(HomeActivity.class, new Intent(ctx, HomeActivity.class)).setup().get(), rnd, 120);
        }
        mash(Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get(), rnd, 200);
        prefs.lang("he");
        mash(Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get(), rnd, 200);
        for (String doc : new String[] {"shacharis", "bentching", "tehillim"}) {
            mash(reader(doc), rnd, 200);
        }
        for (String kind : new String[] {"brachos", "tehillim_month", "tehillim_week", "tehillim_perek", "bookmarks"}) {
            mash(Robolectric.buildActivity(ListActivity.class, new Intent(ctx, ListActivity.class).putExtra("kind", kind)).setup().get(), rnd, 80);
        }
        mash(Robolectric.buildActivity(SetupActivity.class, new Intent(ctx, SetupActivity.class)).setup().get(), rnd, 80);
    }
}
