package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/**
 * Bottom-of-screen checks on a TCL Flip 2-sized screen (240x320 @ mdpi):
 * <ul>
 *   <li>TCL's in-window "MenuBar" strip is refused (feature 15), and hidden if it shows up anyway;</li>
 *   <li>with a real system navigation bar, the content and our soft-key bar stay entirely above it.</li>
 * </ul>
 * Covers every screen: Setup, Home in all 4 layouts, Settings, About, every picker, every list, and Reader.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, qualifiers = "w240dp-h320dp-port-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class SystemBarsTest {
    private static final int W = 240;
    private static final int H = 320;
    private static final int NAV = 30;

    /** Stand-in for TCL's android.widget.MenuBar: same id, class name contains "MenuBar". */
    public static final class FakeTclMenuBar extends FrameLayout {
        public FakeTclMenuBar(Context c) {
            super(c);
            setId(TclMenuBar.MENUBAR_ID);
            setBackgroundColor(0xff474747);
        }
    }

    private Context ctx;
    private Prefs prefs;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("siddur", 0).edit().clear().commit();
        prefs = new Prefs(ctx);
    }

    private static void settle() {
        for (int i = 0; i < 10; i++) {
            ShadowLooper.idleMainLooper();
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
    }

    private BaseActivity launch(Class<? extends BaseActivity> cls, String extraKey, String extraVal) {
        Intent i = new Intent(ctx, cls);
        if (extraKey != null) {
            i.putExtra(extraKey, extraVal);
        }
        BaseActivity a = Robolectric.buildActivity(cls, i).setup().get();
        settle();
        return a;
    }

    /** Every BaseActivity screen, with the label used in failure messages. */
    private List<Object[]> everyScreen() {
        List<Object[]> out = new ArrayList<>();
        out.add(new Object[] {"setup", launch(SetupActivity.class, null, null)});
        prefs.setSetupDone();
        for (String layout : Prefs.LAYOUTS) {
            prefs.layout(layout);
            out.add(new Object[] {"home/" + layout, launch(HomeActivity.class, null, null)});
        }
        out.add(new Object[] {"settings", launch(SettingsActivity.class, null, null)});
        out.add(new Object[] {"about", launch(AboutActivity.class, null, null)});
        for (String k : new String[] {"layout", "theme", "size", "nusach", "lang", "city"}) {
            out.add(new Object[] {"picker/" + k, launch(PickerActivity.class, "kind", k)});
        }
        for (String k : new String[] {"brachos", "tehillim", "tehillim_month", "tehillim_week", "tehillim_perek", "bookmarks"}) {
            out.add(new Object[] {"list/" + k, launch(ListActivity.class, "kind", k)});
        }
        out.add(new Object[] {"reader", launch(ReaderActivity.class, "doc", "shacharis")});
        return out;
    }

    private static int bottomInWindow(View v) {
        int[] xy = new int[2];
        v.getLocationInWindow(xy);
        return xy[1] + v.getHeight();
    }

    private static void layoutDecor(Activity a) {
        View decor = a.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, W, H);
    }

    @Test
    public void everyScreenRefusesTheTclMenuBar() {
        for (Object[] s : everyScreen()) {
            Activity a = (Activity) s[1];
            assertTrue(s[0] + ": TCL FEATURE_NO_MENUBAR (15) not requested",
                    a.getWindow().hasFeature(TclMenuBar.FEATURE_NO_MENUBAR));
        }
    }

    @Test
    public void aTclMenuBarThatShowsUpAnywayIsHidden() {
        for (Object[] s : everyScreen()) {
            BaseActivity a = (BaseActivity) s[1];
            ViewGroup decor = (ViewGroup) a.getWindow().getDecorView();
            // What DecorView.addMenuBar() does on the ROM: appended last (top-most), full width x 30, BOTTOM.
            FakeTclMenuBar bar = new FakeTclMenuBar(a);
            decor.addView(bar, new FrameLayout.LayoutParams(-1, NAV, Gravity.BOTTOM));
            decor.getViewTreeObserver().dispatchOnGlobalLayout();
            assertEquals(s[0] + ": TCL MenuBar still visible", View.GONE, bar.getVisibility());
            // ...and again if the ROM re-shows it later.
            bar.setVisibility(View.VISIBLE);
            decor.getViewTreeObserver().dispatchOnGlobalLayout();
            assertEquals(s[0] + ": re-shown TCL MenuBar not hidden", View.GONE, bar.getVisibility());
        }
    }

    @Test
    public void detectsOnlyTclMenuBars() {
        Activity a = launch(AboutActivity.class, null, null);
        assertTrue(TclMenuBar.isMenuBar(new FakeTclMenuBar(a)));
        View sameIdOtherClass = new View(a);
        sameIdOtherClass.setId(TclMenuBar.MENUBAR_ID);
        assertTrue(!TclMenuBar.isMenuBar(sameIdOtherClass));
        assertTrue(!TclMenuBar.isMenuBar(new FrameLayout(a)));
    }

    @Test
    public void contentAndSoftKeyBarStayAboveASystemNavigationBar() {
        WindowInsets insets = new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, 12, 0, 0))
                .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, NAV))
                .build();
        for (Object[] s : everyScreen()) {
            BaseActivity a = (BaseActivity) s[1];
            View decor = a.getWindow().getDecorView();
            decor.dispatchApplyWindowInsets(insets);
            layoutDecor(a);
            View bar = (View) a.barOk.getParent();
            assertNotNull(bar);
            assertTrue(s[0] + ": soft-key bar has no height", bar.getHeight() > 0);
            assertTrue(s[0] + ": soft-key bar bottom " + bottomInWindow(bar) + " is under the nav bar (limit " + (H - NAV) + ")",
                    bottomInWindow(bar) <= H - NAV);
            assertTrue(s[0] + ": content root bottom " + bottomInWindow(a.top) + " is under the nav bar",
                    bottomInWindow(a.top) <= H - NAV);
            assertTrue(s[0] + ": body collapsed", a.body.getHeight() > 0);
        }
    }

    @Test
    public void withoutASystemBarTheSoftKeyBarIsAtTheBottomEdge() {
        // TCL Flip 2: no system navigation bar, so once TCL's MenuBar is gone our bar is the last thing on screen.
        WindowInsets insets = new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, 12, 0, 0))
                .build();
        BaseActivity a = launch(SettingsActivity.class, null, null);
        a.getWindow().getDecorView().dispatchApplyWindowInsets(insets);
        layoutDecor(a);
        View bar = (View) a.barOk.getParent();
        assertEquals(H, bottomInWindow(bar));
    }
}
