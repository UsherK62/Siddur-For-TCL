package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import java.lang.reflect.Field;
import java.util.List;
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

/** Robolectric smoke tests: every screen launches, and the first-run language / nusach pick sticks. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
@LooperMode(LooperMode.Mode.PAUSED)
public class AppSmokeTest {
    private Context ctx;
    private Prefs prefs;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
    }

    private static void settle() {
        for (int i = 0; i < 20; i++) {
            ShadowLooper.idleMainLooper();
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
    }

    private <T extends Activity> T launch(Class<T> cls, Intent intent) {
        ActivityController<T> c = Robolectric.buildActivity(cls, intent).setup();
        settle();
        return c.get();
    }

    /** Rows of a private RowList field (kept private so the bytecode matches v0.1.0). */
    private static List<View> rows(Object activity, String field) throws Exception {
        Field f = activity.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return ((RowList) f.get(activity)).rows;
    }

    private Intent intent(Class<?> cls) {
        return new Intent(ctx, cls);
    }

    @Test
    public void mainRoutesToSetupOnFirstRun() {
        MainActivity a = launch(MainActivity.class, intent(MainActivity.class));
        Intent next = shadowOf(a).getNextStartedActivity();
        assertNotNull(next);
        assertEquals(SetupActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void mainRoutesToHomeAfterSetup() {
        prefs.setSetupDone();
        MainActivity a = launch(MainActivity.class, intent(MainActivity.class));
        Intent next = shadowOf(a).getNextStartedActivity();
        assertNotNull(next);
        assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void setupPicksLanguageThenNusach() throws Exception {
        SetupActivity a = launch(SetupActivity.class, intent(SetupActivity.class));
        assertEquals(2, rows(a, "list").size()); // English, Hebrew (no Yiddish)
        rows(a, "list").get(1).performClick(); // Hebrew
        settle();
        assertEquals("he", prefs.lang());
        assertFalse(prefs.setupDone());
        assertEquals(3, rows(a, "list").size());
        rows(a, "list").get(1).performClick(); // Sefard
        settle();
        assertEquals("sefard", prefs.nusach());
        assertTrue(prefs.setupDone());
        Intent next = shadowOf(a).getNextStartedActivity();
        assertNotNull(next);
        assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void pickerChangesNusachAndLanguage() throws Exception {
        prefs.setSetupDone();
        PickerActivity n = launch(PickerActivity.class, intent(PickerActivity.class).putExtra("kind", "nusach"));
        rows(n, "rl").get(2).performClick();
        settle();
        assertEquals("edot", prefs.nusach());

        PickerActivity l = launch(PickerActivity.class, intent(PickerActivity.class).putExtra("kind", "lang"));
        assertEquals(2, rows(l, "rl").size());
        rows(l, "rl").get(1).performClick();
        settle();
        assertEquals("he", prefs.lang());
    }

    @Test
    public void everyScreenLaunches() {
        prefs.setSetupDone();
        for (String layout : Prefs.LAYOUTS) {
            prefs.layout(layout);
            assertNotNull(layout, launch(HomeActivity.class, intent(HomeActivity.class)));
        }
        assertNotNull(launch(SettingsActivity.class, intent(SettingsActivity.class)));
        assertNotNull(launch(AboutActivity.class, intent(AboutActivity.class)));
        for (String kind : new String[] {"layout", "theme", "size", "nusach", "lang", "city"}) {
            assertNotNull(kind, launch(PickerActivity.class, intent(PickerActivity.class).putExtra("kind", kind)));
        }
        for (String kind : new String[] {"brachos", "tehillim", "tehillim_month", "tehillim_week", "tehillim_perek", "bookmarks"}) {
            assertNotNull(kind, launch(ListActivity.class, intent(ListActivity.class).putExtra("kind", kind)));
        }
        for (String lang : Prefs.LANGS) {
            prefs.lang(lang);
            assertNotNull(lang, launch(ReaderActivity.class, intent(ReaderActivity.class).putExtra("doc", "shacharis")));
        }
    }

    @Test
    public void everyTextParses() throws Exception {
        String[] docs = {"shacharis", "mincha", "maariv", "bentching", "b_achrona", "b_derech", "b_food", "b_hamita", "b_netilas", "b_sights"};
        for (String nusach : Prefs.NUSACHOS) {
            for (String doc : docs) {
                Doc d = Doc.loadSync(ctx, nusach, doc);
                assertNotNull(nusach + "/" + doc, d);
                assertFalse(nusach + "/" + doc, d.items.isEmpty());
            }
        }
        Doc t = Doc.loadSync(ctx, "ashkenaz", "tehillim");
        assertFalse(t.items.isEmpty());
    }
}
