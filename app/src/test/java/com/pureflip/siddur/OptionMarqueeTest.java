package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import java.util.concurrent.TimeUnit;
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

/** Popup options that don't fit scroll while focused. Needs real text measuring (native graphics). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class OptionMarqueeTest {
    private Context ctx;
    private Prefs prefs;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
    }

    private static void settle() {
        SettingsD1Test.settle();
    }

    private SettingsActivity settings() {
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        settle();
        return a;
    }

    @Test
    public void longPopupOptionScrollsWhileFocused() {
        for (String lang : new String[] {"en", "he"}) {
            prefs.lang(lang);
            SettingsActivity a = settings();
            int bar = ((View) a.barOk.getParent()).getHeight();
            ChoicePopup p = new ChoicePopup(a, a.top, a.pal, lang, "T", bar)
                    .option("en".equals(lang) ? "A very long option name that cannot fit" : "שם ארוך מאוד שלא נכנס בשורה אחת בכלל", null)
                    .option("Short", null);
            p.show(1);
            settle();
            OptionRow longRow = p.rows.get(0);
            assertFalse(longRow.scrolling());
            p.focus(0);
            ShadowLooper.idleMainLooper(Marquee.DELAY_MS + 600, TimeUnit.MILLISECONDS);
            assertTrue(longRow.scrolling());
            assertTrue(longRow.scrollOffset() > 0);
            assertFalse(p.rows.get(1).scrolling());
            p.focus(1);
            ShadowLooper.idleMainLooper(50, TimeUnit.MILLISECONDS);
            assertFalse(longRow.scrolling());
            assertEquals(0.0f, longRow.scrollOffset(), 0.0f);
            p.dismiss(null);
            settle();
        }
    }

    @Test
    public void nowCardGlidesAndComesBack() {
        for (String lang : new String[] {"en", "he"}) {
            prefs.lang(lang);
            prefs.layout("list");
            HomeActivity h = Robolectric.buildActivity(HomeActivity.class, new Intent(ctx, HomeActivity.class)).setup().get();
            settle();
            Line now = null;
            for (View v : PopupAgainMarqueeTest.all(h.getWindow().getDecorView())) {
                if (v instanceof Line && ((Line) v).getText().toString().startsWith("Now")) {
                    now = (Line) v;
                }
            }
            org.junit.Assert.assertNotNull(now);
            float avail = now.getWidth() - now.getCompoundPaddingLeft() - now.getCompoundPaddingRight();
            boolean cut = now.getPaint().measureText(now.getText().toString()) > avail + 0.5f;
            assertEquals(cut, now.scrolling());
            if (cut) {
                ShadowLooper.idleMainLooper(Marquee.DELAY_MS + 500, TimeUnit.MILLISECONDS);
                assertTrue(now.scrollOffset() > 0);
            }
        }
    }

    @Test
    public void everyThemeNameFitsWithoutScrolling() {
        for (String lang : new String[] {"en", "he"}) {
            prefs.lang(lang);
            SettingsActivity a = settings();
            a.focusRow(1);
            settle();
            a.rows.get(1).performClick();
            settle();
            ChoicePopup p = a.choice();
            for (int i = 0; i < p.rows.size(); i++) {
                p.focus(i);
                ShadowLooper.idleMainLooper(Marquee.DELAY_MS + 300, TimeUnit.MILLISECONDS);
                assertFalse(lang + " " + p.rows.get(i).label(), p.rows.get(i).scrolling());
            }
        }
    }
}
