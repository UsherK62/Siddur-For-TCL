package com.pureflip.siddur;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowLooper;

/**
 * 0.2.1 review pictures at 240x320 (Robolectric native graphics, not the phone): the grouped Theme picker in
 * English and Hebrew, top and scrolled, and the home Now card at rest and mid-scroll.
 * Output: system property "review.dir", default build/review-0.2.1.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class Review021ScreenshotTest {
    private Context ctx;
    private Prefs prefs;
    private File dir;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
        dir = new File(System.getProperty("review.dir", "build/review-0.2.1"));
        dir.mkdirs();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
    }

    @After
    public void tearDown() {
        ShadowChoreographer.setPaused(false);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(1));
    }

    private void shoot(BaseActivity a, String name) throws Exception {
        View root = a.getWindow().getDecorView();
        Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        File f = new File(dir, name + ".png");
        try (FileOutputStream out = new FileOutputStream(f)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        assertTrue(f.length() > 0);
    }

    private SettingsActivity themePopup(String lang, String theme) {
        prefs.lang(lang);
        prefs.theme(theme);
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        SettingsD1Test.settle();
        a.focusRow(1);
        SettingsD1Test.settle();
        a.rows.get(1).performClick();
        SettingsD1Test.settle();
        return a;
    }

    @Test
    public void themePicker() throws Exception {
        for (String lang : new String[] {"en", "he"}) {
            SettingsActivity a = themePopup(lang, "forest");
            shoot(a, "theme-" + lang + "-1-top");
            ChoicePopup p = a.choice();
            p.focus(Pal.LIGHT.length); // first dark theme: its header scrolls in with it
            SettingsD1Test.settle();
            shoot(a, "theme-" + lang + "-2-dark-group");
            p.focus(p.rows.size() - 1);
            SettingsD1Test.settle();
            shoot(a, "theme-" + lang + "-3-bottom");
        }
        // a dark theme in use: the picker opens scrolled to it
        SettingsActivity a = themePopup("en", "teal");
        shoot(a, "theme-en-4-opens-on-dark-green");
    }

    @Test
    public void homeNowCard() throws Exception {
        // Real-time frames (16 ms) so the marquee can be caught mid-glide.
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
        for (String lang : new String[] {"en", "he"}) {
            prefs.lang(lang);
            prefs.theme("forest");
            prefs.layout("list");
            HomeActivity h = Robolectric.buildActivity(HomeActivity.class, new Intent(ctx, HomeActivity.class)).setup().get();
            ShadowLooper.idleMainLooper(700, TimeUnit.MILLISECONDS); // screen faded in, marquee still waiting
            Line now = null;
            for (View v : PopupAgainMarqueeTest.all(h.getWindow().getDecorView())) {
                if (v instanceof Line && ((Line) v).getText().toString().startsWith("Now")) {
                    now = (Line) v;
                }
            }
            float dist = now.getPaint().measureText(now.getText().toString())
                    - (now.getWidth() - now.getCompoundPaddingLeft() - now.getCompoundPaddingRight());
            if (dist <= 0) {
                shoot(h, "home-" + lang + "-1-now-card-at-rest");
                continue; // fits: nothing scrolls
            }
            // Marquee: waits 0.9 s, glides at 30 dp/s, holds 1.1 s, starts over.
            for (int i = 0; i < 300 && now.scrollOffset() < dist * 0.45f; i++) {
                ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
            }
            shoot(h, "home-" + lang + "-2-now-card-mid-scroll");
            for (int i = 0; i < 300 && now.scrollOffset() < dist - 0.5f; i++) {
                ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
            }
            shoot(h, "home-" + lang + "-3-now-card-scrolled-to-end");
            // after the hold it jumps back and waits with the "…" text: the resting look
            for (int i = 0; i < 300 && now.scrollOffset() > 0; i++) {
                ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
            }
            ShadowLooper.idleMainLooper(200, TimeUnit.MILLISECONDS);
            shoot(h, "home-" + lang + "-1-now-card-at-rest");
        }
    }

    /**
     * RTL check: a Hebrew label too long for its row (a test label, not app text) in a reader popup
     * starts on the right with "…" and glides so its left end comes into view.
     */
    @Test
    public void hebrewPopupRowMarquee() throws Exception {
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
        prefs.lang("he");
        prefs.theme("forest");
        ReaderActivity r = Robolectric.buildActivity(ReaderActivity.class, new Intent(ctx, ReaderActivity.class).putExtra("doc", "mincha")).setup().get();
        for (int i = 0; i < 100 && Doc.cached(prefs.nusach(), "mincha") == null; i++) {
            Thread.sleep(20);
            ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
        }
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        Popup p = r.newPopup("בדיקה");
        p.ui("אשרי יושבי ביתך עוד יהללוך סלה, אשרי העם שככה לו", null);
        p.ui("קצר", null);
        p.show(0);
        ShadowLooper.idleMainLooper(400, TimeUnit.MILLISECONDS);
        java.lang.reflect.Field lf = Popup.class.getDeclaredField("list");
        lf.setAccessible(true);
        RowList list = (RowList) lf.get(p);
        Line l = null;
        for (View v : PopupAgainMarqueeTest.all(list.rows.get(0))) {
            if (v instanceof Line) {
                l = (Line) v;
            }
        }
        float dist = l.getPaint().measureText(l.getText().toString()) - (l.getWidth() - l.getCompoundPaddingLeft() - l.getCompoundPaddingRight());
        assertTrue(dist > 0);
        for (int i = 0; i < 400 && l.scrollOffset() < dist * 0.5f; i++) {
            ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
        }
        shoot(r, "rtl-check-he-popup-2-mid-scroll");
        for (int i = 0; i < 600 && l.scrollOffset() < dist - 0.5f; i++) {
            ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
        }
        shoot(r, "rtl-check-he-popup-3-end");
        for (int i = 0; i < 300 && l.scrollOffset() > 0; i++) {
            ShadowLooper.idleMainLooper(16, TimeUnit.MILLISECONDS);
        }
        ShadowLooper.idleMainLooper(200, TimeUnit.MILLISECONDS);
        shoot(r, "rtl-check-he-popup-1-at-rest");
    }
}
