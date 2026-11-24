package com.pureflip.siddur;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/** Settings design D1 (Auto Reply style, RTL), the ten themes, theme / language migration, no Yiddish. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class SettingsD1Test {
    private Context ctx;
    private Prefs prefs;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
        prefs.setSetupDone();
    }

    static void settle() {
        for (int i = 0; i < 20; i++) {
            ShadowLooper.idleMainLooper();
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
    }

    private ActivityController<SettingsActivity> controller() {
        ActivityController<SettingsActivity> c = Robolectric.buildActivity(SettingsActivity.class, new Intent(ctx, SettingsActivity.class)).setup();
        settle();
        return c;
    }

    private static void key(SettingsActivity a, int code) {
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        settle();
    }

    private int dp(float v) {
        return Ui.dp(ctx, v);
    }

    private static int focusedIndex(SettingsActivity a) {
        return a.rows.indexOf(a.getWindow().getDecorView().findFocus());
    }

    @Test
    public void structureIsThreeCardsWithBilingualHeaders() {
        SettingsActivity a = controller().get();
        String[] keys = new String[a.rows.size()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = a.rows.get(i).key;
        }
        assertArrayEquals(new String[] {"layout", "theme", "fontSize", "nusach", "city", "israel", "language", "about"}, keys);
        View root = a.getWindow().getDecorView();
        for (String en : new String[] {"Display", "Siddur", "General"}) {
            assertNotNull(en, root.findViewWithTag("head:" + en));
            assertNotNull(en, root.findViewWithTag("card:" + en));
        }
        // Language lives in the General card.
        assertEquals(root.findViewWithTag("card:General"), a.row("language").getParent());
        // Hebrew UI: label on the right, value on the left.
        prefs.lang("he");
        a = controller().get();
        SetRow r = a.row("layout");
        assertTrue(r.label.getLeft() > r.value().getLeft());
    }

    @Test
    public void focusedRowExpandsAndHidesItsDividers() {
        SettingsActivity a = controller().get();
        assertEquals(0, focusedIndex(a));
        assertEquals(dp(34), a.rows.get(0).getHeight());
        assertEquals(dp(25), a.rows.get(1).getHeight());
        assertTrue(a.dividerHidden(0));
        assertFalse(a.dividerHidden(1));

        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals(1, focusedIndex(a));
        assertEquals(dp(25), a.rows.get(0).getHeight());
        assertEquals(dp(34), a.rows.get(1).getHeight());
        assertTrue(a.dividerHidden(0));
        assertTrue(a.dividerHidden(1));

        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals(2, focusedIndex(a));
        assertFalse(a.dividerHidden(0));
        assertTrue(a.dividerHidden(1));
    }

    @Test
    public void smoothScrollKeepsFocusedRowInView() {
        SettingsActivity a = controller().get();
        key(a, KeyEvent.KEYCODE_DPAD_UP); // wraps to the last row (About)
        assertEquals(a.rows.size() - 1, focusedIndex(a));
        assertInView(a, a.rows.get(a.rows.size() - 1));
        for (int i = 0; i < a.rows.size(); i++) {
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
            assertInView(a, a.rows.get(focusedIndex(a)));
        }
        key(a, KeyEvent.KEYCODE_DPAD_DOWN); // last -> first: back to the very top (first header shows)
        assertEquals(0, focusedIndex(a));
        assertEquals(0, scroller(a).getScrollY());
    }

    private ScrollView scroller(SettingsActivity a) {
        View v = a.rows.get(0);
        while (!(v instanceof ScrollView)) {
            v = (View) v.getParent();
        }
        return (ScrollView) v;
    }

    private void assertInView(SettingsActivity a, View row) {
        ScrollView s = scroller(a);
        Rect r = new Rect(0, 0, row.getWidth(), row.getHeight());
        ((ViewGroup) s.getChildAt(0)).offsetDescendantRectToMyCoords(row, r);
        int y = s.getScrollY();
        assertTrue("row top " + r.top + " above view " + y, r.top >= y);
        assertTrue("row bottom " + r.bottom + " below view " + (y + s.getHeight()), r.bottom + dp(4) <= y + s.getHeight() + 1);
    }

    @Test
    public void focusIndexSurvivesRebuildAndRecreate() {
        ActivityController<SettingsActivity> c = controller();
        SettingsActivity a = c.get();
        for (int i = 0; i < 4; i++) {
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        }
        assertEquals(4, focusedIndex(a));
        c.pause().resume(); // onResume rebuilds the list
        settle();
        assertEquals(4, focusedIndex(c.get()));
        c.recreate();
        settle();
        assertEquals(4, focusedIndex(c.get()));
        assertEquals(dp(34), c.get().rows.get(4).getHeight());
    }

    @Test
    public void israelSwitchFlipsOnOk() {
        SettingsActivity a = controller().get();
        while (focusedIndex(a) != 5) {
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        }
        assertFalse(prefs.israel());
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        assertTrue(prefs.israel());
        assertEquals("jerusalem", prefs.city());
        assertTrue(((Marks.Toggle) a.row("israel").value()).isOn());
        assertTrue(a.choice() == null || !a.choice().isShowing()); // no popup for on/off rows
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        assertFalse(prefs.israel());
        assertEquals("brooklyn", prefs.city());
    }

    @Test
    public void layoutPickerIsACentredPopupWithRadios() {
        SettingsActivity a = controller().get();
        key(a, KeyEvent.KEYCODE_DPAD_CENTER); // Layout row is focused first
        ChoicePopup p = a.choice();
        assertNotNull(p);
        assertTrue(p.isShowing());
        assertEquals(4, p.rows.size());
        assertEquals(3, p.checked()); // "list" is the default
        assertTrue(p.rows.get(3).hasFocus());
        key(a, KeyEvent.KEYCODE_DPAD_DOWN); // wraps to the first option
        assertTrue(p.rows.get(0).hasFocus());
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        assertFalse(p.isShowing());
        assertEquals("carousel", prefs.layout());
        assertEquals(0, focusedIndex(a)); // back on the Layout row
        assertFalse(a.isFinishing());
    }

    @Test
    public void backClosesPopupNotScreen() {
        SettingsActivity a = controller().get();
        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        key(a, KeyEvent.KEYCODE_DPAD_CENTER); // Theme
        assertTrue(a.choice().isShowing());
        assertEquals(Pal.ALL.length, a.choice().rows.size());
        key(a, KeyEvent.KEYCODE_BACK);
        assertFalse(a.choice().isShowing());
        assertFalse(a.isFinishing());
        assertEquals("forest", prefs.theme());
        assertEquals(1, focusedIndex(a));
    }

    @Test
    public void themePickChangesTheme() {
        SettingsActivity a = controller().get();
        key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        a.choice().pick(java.util.Arrays.asList(SettingsActivity.themeOrder()).indexOf("teal"));
        settle();
        assertEquals("teal", prefs.theme());
    }

    @Test
    public void languagePickerOffersOnlyEnglishAndHebrew() {
        SettingsActivity a = controller().get();
        while (focusedIndex(a) != 6) {
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        }
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        assertEquals(2, a.choice().rows.size());
        assertEquals("English", a.choice().rows.get(0).label().toString());
        assertEquals("עברית", a.choice().rows.get(1).label().toString());
        a.choice().pick(1);
        settle();
        assertEquals("he", prefs.lang());
    }

    @Test
    public void cityPopupScrollsAndFitsAboveTheBar() {
        SettingsActivity a = controller().get();
        while (focusedIndex(a) != 4) {
            key(a, KeyEvent.KEYCODE_DPAD_DOWN);
        }
        key(a, KeyEvent.KEYCODE_DPAD_CENTER);
        ChoicePopup p = a.choice();
        assertEquals(Cities.ALL.length, p.rows.size());
        View card = (View) p.rows.get(0).getParent().getParent().getParent();
        int[] loc = new int[2];
        card.getLocationInWindow(loc);
        View bar = (View) a.barOk.getParent();
        int[] bl = new int[2];
        bar.getLocationInWindow(bl);
        assertTrue("popup bottom " + (loc[1] + card.getHeight()) + " over bar " + bl[1], loc[1] + card.getHeight() <= bl[1]);
        assertTrue(loc[1] >= 0);
        p.rows.get(Cities.ALL.length - 1).requestFocus();
        settle();
        p.pick(Cities.ALL.length - 1);
        settle();
        assertEquals(Cities.ALL[Cities.ALL.length - 1].id, prefs.city());
    }

    @Test
    public void tenThemesWithUniqueNames() {
        assertEquals(10, Pal.ALL.length);
        Set<String> ids = new HashSet<>();
        Set<String> he = new HashSet<>();
        Set<String> en = new HashSet<>();
        for (Pal p : Pal.ALL) {
            assertTrue(ids.add(p.id));
            String h = Str.get("he", "t_" + p.id);
            String e = Str.get("en", "t_" + p.id);
            assertFalse(h.equals("t_" + p.id));
            assertTrue(h, h.matches("[\\u0590-\\u05FF ]+"));
            assertTrue(e, e.matches("[A-Za-z ]+"));
            assertTrue(he.add(h));
            assertTrue(en.add(e));
        }
        assertEquals("שחור וזהב", Str.get("he", "t_amber"));
        assertEquals("לבן", Str.get("he", "t_ar_light"));
        // the picker lists every theme exactly once: light group, then dark group, each by colour family
        Set<String> listed = new HashSet<>(java.util.Arrays.asList(SettingsActivity.themeOrder()));
        assertEquals(10, SettingsActivity.themeOrder().length);
        assertEquals(10, listed.size());
        for (String id : Pal.LIGHT) {
            assertFalse(id, Pal.of(id).dark);
        }
        for (String id : Pal.DARK) {
            assertTrue(id, Pal.of(id).dark);
        }
        assertEquals(0xFFF2F3F7, Pal.of("ar_light").bg);
        assertEquals(0xFF0E1014, Pal.of("ar_dark").bg);
    }

    @Test
    public void everyThemeRendersEveryScreen() {
        for (Pal p : Pal.ALL) {
            prefs.theme(p.id);
            for (String layout : Prefs.LAYOUTS) {
                prefs.layout(layout);
                assertNotNull(p.id, Robolectric.buildActivity(HomeActivity.class, new Intent(ctx, HomeActivity.class)).setup().get());
                settle();
            }
            SettingsActivity s = controller().get();
            assertEquals(p.id, s.pal.id);
            assertNotNull(Robolectric.buildActivity(ReaderActivity.class, new Intent(ctx, ReaderActivity.class).putExtra("doc", "mincha")).setup().get());
            settle();
        }
    }

    @Test
    public void savedThemesMigrate() {
        String[][] cases = {{"forest", "forest"}, {"cream", "cream"}, {"amber", "amber"}, {"cream_sage", "cream"},
                {"amber_dark", "amber"}, {"burgundy", "forest"}, {"teal", "teal"}};
        for (String[] c : cases) {
            ctx.getSharedPreferences("siddur", 0).edit().putString("theme", c[0]).commit();
            assertEquals(c[0], c[1], prefs.theme());
            assertEquals(c[1], ctx.getSharedPreferences("siddur", 0).getString("theme", null));
        }
    }

    @Test
    public void yiddishIsGoneAndMigratesToHebrew() {
        assertArrayEquals(new String[] {"en", "he"}, Prefs.LANGS);
        ctx.getSharedPreferences("siddur", 0).edit().putString("lang", "yi").commit();
        assertEquals("he", prefs.lang());
        assertEquals("he", ctx.getSharedPreferences("siddur", 0).getString("lang", null));
        assertEquals("עברית", SettingsActivity.langName("he"));
        assertEquals("English", SettingsActivity.langName("en"));
        assertEquals("ברוקלין", Cities.of("brooklyn").name("he"));
        assertEquals("שבת", Str.week("he", 6));
        // Saved "yi" users land on a Hebrew screen without crashing.
        SettingsActivity a = controller().get();
        assertEquals("he", a.lang);
    }
}
