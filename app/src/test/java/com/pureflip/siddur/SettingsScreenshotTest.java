package com.pureflip.siddur;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;

/**
 * Renders the real Settings screen (Robolectric native graphics) at 240x320 into PNGs for a visual check.
 * Output dir: system property "shots.dir", default build/screens.
 * The phone's status bar is not part of the app window, so it isn't in the pictures.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SettingsScreenshotTest {
    private Context ctx;
    private Prefs prefs;
    private File dir;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
        dir = new File(System.getProperty("shots.dir", "build/screens"));
        dir.mkdirs();
    }

    private SettingsActivity open(String theme, String lang) {
        prefs.theme(theme);
        prefs.lang(lang);
        // A flip phone has no touch screen: keep the window out of touch mode so rows can take focus.
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        SettingsD1Test.settle();
        return a;
    }

    private void shoot(SettingsActivity a, String name) throws Exception {
        View root = a.getWindow().getDecorView();
        Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        File f = new File(dir, name + ".png");
        try (FileOutputStream out = new FileOutputStream(f)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        assertTrue(f.length() > 0);
    }

    /** Key routing is covered by SettingsD1Test; here we drive focus directly (native-graphics windows don't take key focus). */
    private static void focus(SettingsActivity a, int i) {
        a.focusRow(i);
        SettingsD1Test.settle();
    }

    private static void ok(SettingsActivity a, int i) {
        a.rows.get(i).performClick();
        SettingsD1Test.settle();
    }

    @Test
    public void settingsScreens() throws Exception {
        SettingsActivity a = open("forest", "he");
        focus(a, 1); // Theme
        shoot(a, "1-settings-forest-he-theme-focused");

        focus(a, 0);
        ok(a, 0); // Layout popup
        shoot(a, "2-settings-forest-he-layout-popup");

        a = open("ar_dark", "he");
        focus(a, 7);
        focus(a, 6); // Language, list scrolled to the bottom
        shoot(a, "3-settings-ardark-he-scrolled-language");

        a = open("cream", "en");
        for (int i = 1; i <= 5; i++) {
            focus(a, i);
        }
        ok(a, 5); // In Israel -> on
        shoot(a, "4-settings-cream-en-israel-on");

        a = open("teal", "he");
        focus(a, 1);
        ok(a, 1); // Theme popup
        shoot(a, "5-settings-teal-he-theme-popup");
    }

    private void shootAny(Class<? extends BaseActivity> cls, Intent extra, String name) throws Exception {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
        BaseActivity a = Robolectric.buildActivity(cls, extra).setup().get();
        for (int i = 0; i < 15; i++) { // the reader loads its text on a worker thread
            SettingsD1Test.settle();
            Thread.sleep(40);
        }
        SettingsD1Test.settle();
        View root = a.getWindow().getDecorView();
        Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    /** The new themes on the other screens (home, reader), for a visual check. */
    @Test
    public void otherScreens() throws Exception {
        prefs.lang("he");
        String[][] cases = {{"forest", "list"}, {"cream", "toc"}, {"ar_dark", "carousel"}, {"coffee", "split"}};
        for (String[] c : cases) {
            prefs.theme(c[0]);
            prefs.layout(c[1]);
            shootAny(HomeActivity.class, new Intent(ctx, HomeActivity.class), "home-" + c[0] + "-" + c[1]);
        }
        prefs.theme("sky");
        shootAny(ReaderActivity.class, new Intent(ctx, ReaderActivity.class).putExtra("doc", "mincha"), "reader-sky");
    }
}
