package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/** 0.2.1: the key that opened a popup closes it again, one-line labels scroll, themes come in two groups. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class PopupAgainMarqueeTest {
    private Context ctx;
    private Prefs prefs;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
        prefs.lang("en");
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
    }

    private static void settle() {
        SettingsD1Test.settle();
    }

    private static void key(BaseActivity a, int code) {
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        settle();
    }

    private SettingsActivity settings() {
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup().get();
        settle();
        return a;
    }

    private static int focusedRow(SettingsActivity a) {
        return a.rows.indexOf(a.getWindow().getDecorView().findFocus());
    }

    private ReaderActivity reader() throws Exception {
        ReaderActivity r = Robolectric.buildActivity(ReaderActivity.class, new Intent(ctx, ReaderActivity.class).putExtra("doc", "mincha")).setup().get();
        for (int i = 0; i < 100 && Doc.cached(prefs.nusach(), "mincha") == null; i++) {
            Thread.sleep(20);
            ShadowLooper.idleMainLooper();
        }
        settle();
        return r;
    }

    // ------------------------------------------------------------------ press again to close

    @Test
    public void okOpensSettingsPickerAndOkAgainClosesItUnchanged() {
        String[] keys = {"layout", "theme", "fontSize", "nusach", "city"};
        for (int row = 0; row < keys.length; row++) {
            SettingsActivity a = settings();
            while (focusedRow(a) != row) {
                key(a, KeyEvent.KEYCODE_DPAD_DOWN);
            }
            String before = prefs.layout() + prefs.theme() + prefs.size() + prefs.nusach() + prefs.city();
            key(a, KeyEvent.KEYCODE_DPAD_CENTER);
            assertTrue(keys[row], a.choice().isShowing());
            key(a, KeyEvent.KEYCODE_DPAD_CENTER); // same key again, on the option it opened on
            assertFalse(keys[row], a.choice().isShowing());
            assertEquals(keys[row], before, prefs.layout() + prefs.theme() + prefs.size() + prefs.nusach() + prefs.city());
            assertEquals(keys[row], row, focusedRow(a));
            assertEquals(Str.get(a.lang, "back"), a.barRight.getText().toString());
            assertFalse(a.isFinishing());
        }
    }

    @Test
    public void okOnAnotherOptionStillPicksIt() {
        SettingsActivity a = settings();
        key(a, KeyEvent.KEYCODE_DPAD_CENTER); // Layout
        key(a, KeyEvent.KEYCODE_DPAD_UP);
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        assertFalse(a.choice().isShowing());
        assertEquals(Prefs.LAYOUTS[2], prefs.layout());
    }

    @Test
    public void okWithNothingFocusedClosesThePicker() {
        SettingsActivity a = settings();
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        View f = a.getWindow().getDecorView().findFocus();
        assertTrue(a.choice().rows.contains(f));
        f.clearFocus();
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        assertFalse(a.choice().isShowing());
        assertEquals("list", prefs.layout());
    }

    @Test
    public void softKeysCloseTheSettingsPicker() {
        for (int code : new int[] {KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_SOFT_RIGHT, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK}) {
            SettingsActivity a = settings();
            key(a, KeyEvent.KEYCODE_DPAD_CENTER);
            assertTrue(a.choice().isShowing());
            key(a, code);
            assertFalse("key " + code, a.choice().isShowing());
            assertFalse("key " + code, a.isFinishing());
        }
    }

    @Test
    public void okUpLeftOverFromOpeningDoesNotPick() {
        SettingsActivity a = settings();
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        key(a, KeyEvent.KEYCODE_DPAD_UP);
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER)); // no matching down
        settle();
        assertTrue(a.choice().isShowing());
        assertEquals("list", prefs.layout());
    }

    @Test
    public void readerMenuKeyAgainClosesTheMenu() throws Exception {
        for (int code : new int[] {KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_MENU}) {
            ReaderActivity r = reader();
            key(r, code);
            assertNotNull(r.popup);
            assertTrue(r.popup.isShowing());
            key(r, code);
            assertFalse(r.popup.isShowing());
            assertFalse(r.isFinishing());
        }
    }

    @Test
    public void readerTextSizeOkOnCurrentSizeJustCloses() throws Exception {
        ReaderActivity r = reader();
        key(r, KeyEvent.KEYCODE_SOFT_LEFT);
        key(r, KeyEvent.KEYCODE_DPAD_DOWN);
        key(r, KeyEvent.KEYCODE_DPAD_DOWN); // Font size
        key(r, KeyEvent.KEYCODE_DPAD_CENTER);
        Popup sizes = r.popup;
        assertTrue(sizes.isShowing());
        String was = prefs.size();
        key(r, KeyEvent.KEYCODE_DPAD_CENTER); // opens on the current size
        assertFalse(sizes.isShowing());
        assertEquals(was, prefs.size());
        assertFalse(r.isFinishing());
    }

    // ------------------------------------------------------------------ themes

    @Test
    public void themePickerHasLightAndDarkGroupsInColourOrder() {
        for (String lang : new String[] {"en", "he"}) {
            prefs.lang(lang);
            SettingsActivity a = settings();
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
            key(a, KeyEvent.KEYCODE_DPAD_CENTER);
            ChoicePopup p = a.choice();
            assertEquals(10, p.rows.size());
            String[] order = SettingsActivity.themeOrder();
            for (int i = 0; i < order.length; i++) {
                assertEquals(Str.get(lang, "t_" + order[i]), p.rows.get(i).label().toString());
                TextView h = p.heads.get(i);
                if (i == 0) {
                    assertEquals("en".equals(lang) ? "Light" : "בהיר", h.getText().toString());
                } else if (i == Pal.LIGHT.length) {
                    assertEquals("en".equals(lang) ? "Dark" : "כהה", h.getText().toString());
                } else {
                    assertNull(h);
                }
            }
            assertFalse(p.heads.get(0).isFocusable());
            // Up from the first option wraps to the last one (headers are skipped), Down wraps back
            assertTrue(p.rows.get(p.checked()).hasFocus()); // opens on the theme in use
            p.focus(0);
            settle();
            key(a, KeyEvent.KEYCODE_DPAD_UP);
            assertTrue(p.rows.get(order.length - 1).hasFocus());
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
            assertTrue(p.rows.get(0).hasFocus());
            // Down from the last light theme lands on the first dark one
            p.focus(Pal.LIGHT.length - 1);
            settle();
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
            assertTrue(p.rows.get(Pal.LIGHT.length).hasFocus());
            if ("en".equals(lang)) {
                assertEquals("White", p.rows.get(0).label().toString());
                assertEquals("Black", p.rows.get(Pal.LIGHT.length).label().toString());
            }
        }
    }

    @Test
    public void themePickerFitsAboveTheBar() {
        SettingsActivity a = settings();
        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        ChoicePopup p = a.choice();
        View card = (View) p.rows.get(0).getParent().getParent().getParent();
        int[] loc = new int[2];
        card.getLocationInWindow(loc);
        int[] bl = new int[2];
        ((View) a.barOk.getParent()).getLocationInWindow(bl);
        assertTrue(loc[1] >= 0);
        assertTrue(loc[1] + card.getHeight() <= bl[1]);
    }

    @Test
    public void focusedThemeRowAndItsHeaderAreFullyInView() {
        SettingsActivity a = settings();
        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        ChoicePopup p = a.choice();
        for (int i = 0; i < p.rows.size(); i++) {
            p.focus(i);
            settle();
            View row = p.rows.get(i);
            View scroll = (View) row.getParent().getParent();
            int y = scroll.getScrollY();
            assertTrue("row " + i + " top", row.getTop() >= y);
            assertTrue("row " + i + " bottom", row.getBottom() <= y + scroll.getHeight());
            if (p.heads.get(i) != null) {
                assertTrue("header " + i, p.heads.get(i).getTop() >= y);
            }
        }
    }

    // ------------------------------------------------------------------ one-line labels scroll

    @Test
    public void nowCardScrollsAndFocusedRowsScroll() {
        prefs.layout("list");
        HomeActivity h = Robolectric.buildActivity(HomeActivity.class, new Intent(ctx, HomeActivity.class)).setup().get();
        settle();
        int lines = 0;
        View now = null;
        for (View v : all(h.getWindow().getDecorView())) {
            if (v instanceof Line && ((Line) v).getText().toString().startsWith("Now")) {
                now = v;
            }
            if (v instanceof Line) {
                lines++;
            }
        }
        assertTrue(lines > 0);
        assertNotNull(now);
        assertTrue(((Line) now).scrolls());
        assertEquals(TextUtils.TruncateAt.END, ((Line) now).getEllipsize()); // "…" while it waits
        // the Now card keeps scrolling after focus moves on
        key(h, KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(((Line) now).scrolls());
    }

    @Test
    public void settingsRowLabelScrollsOnlyWhileFocused() {
        SettingsActivity a = settings();
        Line l0 = (Line) a.rows.get(0).label;
        Line l1 = (Line) a.rows.get(1).label;
        assertTrue(l0.scrolls());
        assertFalse(l1.scrolls());
        assertEquals(TextUtils.TruncateAt.END, l1.getEllipsize());
        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        assertFalse(l0.scrolls());
        assertTrue(l1.scrolls());
    }

    static java.util.List<View> all(View v) {
        java.util.List<View> out = new java.util.ArrayList<>();
        out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                out.addAll(all(g.getChildAt(i)));
            }
        }
        return out;
    }
}
