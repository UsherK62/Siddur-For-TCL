package com.pureflip.siddur;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Keeps the TCL Flip 2 / PureFlip "MenuBar" out of our windows.
 *
 * <p>On this ROM every {@code Activity.setContentView()} calls {@code PhoneWindow.enableMenuBar(true)} and then
 * {@code DecorView.addMenuBar()}. That adds TCL's own soft-key strip, an {@code android.widget.MenuBar} (id
 * {@code 0x01020062}, full width x 30 px, gravity BOTTOM), as the <b>last, top-most child of our own DecorView</b>.
 * Its background is the theme's {@code ?android:navigationBarColor}. Its labels are filled only by apps that call
 * TCL's private MenuBar API, so for us it is a "dead" strip drawn over the bottom 30 px of the window, on top of our
 * own Menu / OK / Back bar.
 *
 * <p>It is not a system bar: it reports no window insets, and immersive flags and fitsSystemWindows don't affect it.
 * This is the same fix the other PureFlip apps use (PureFlip Helper's {@code PfWin} and the Tutorial):
 * <ol>
 *   <li>request TCL's window feature 15 ({@code FEATURE_NO_MENUBAR}) before {@code setContentView};</li>
 *   <li>call {@code Activity.removeMenuBar()} after it;</li>
 *   <li>a layout-time guard sets any MenuBar that shows up again to GONE.</li>
 * </ol>
 * The theme also makes {@code navigationBarColor} transparent, so a MenuBar that survives all three is see-through.
 *
 * <p>Every member used here is whitelisted in the ROM framework, so reflection isn't blocked. On any other phone each
 * step is a harmless no-op: an unused feature bit, a missing method, and no matching view.
 */
final class TclMenuBar {
    /** TCL addition to {@code Window}: {@code FEATURE_MENUBAR = 14}, {@code FEATURE_NO_MENUBAR = 15}. */
    static final int FEATURE_NO_MENUBAR = 15;
    /** {@code com.android.internal.R.id.decor_menu_bar}. */
    static final int MENUBAR_ID = 0x01020062;

    private TclMenuBar() {
    }

    /** Call before {@code setContentView}. */
    static void beforeContent(Activity a) {
        try {
            a.requestWindowFeature(FEATURE_NO_MENUBAR);
        } catch (Throwable ignored) {
        }
    }

    /** Call right after {@code setContentView}. */
    static void afterContent(final Activity a) {
        try {
            Activity.class.getMethod("removeMenuBar").invoke(a);
        } catch (Throwable ignored) {
        }
        hide(a);
        try {
            a.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            hide(a);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /** Hides every TCL MenuBar in the window. Returns how many were visible. */
    static int hide(Activity a) {
        View decor = a.getWindow().peekDecorView();
        return decor == null ? 0 : hideIn(decor);
    }

    private static int hideIn(View v) {
        if (isMenuBar(v)) {
            if (v.getVisibility() == View.GONE) {
                return 0;
            }
            v.setVisibility(View.GONE);
            return 1;
        }
        int n = 0;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                n += hideIn(g.getChildAt(i));
            }
        }
        return n;
    }

    static boolean isMenuBar(View v) {
        for (Class<?> c = v.getClass(); c != null; c = c.getSuperclass()) {
            if ("android.widget.MenuBar".equals(c.getName())) {
                return true;
            }
        }
        return v.getId() == MENUBAR_ID && v.getClass().getName().contains("MenuBar");
    }
}
