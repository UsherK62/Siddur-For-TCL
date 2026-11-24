package com.pureflip.siddur;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int MATCH = -1;
    static final int WRAP = -2;
    static Typeface frank;
    static Typeface frankBold;
    static Typeface plex;
    static Typeface plexBold;
    static Typeface plexSemi;
    /** Settings (Auto Reply style): Inter for Latin text, Heebo for Hebrew. Subset, OFL (assets/fonts/OFL-*.txt). */
    static Typeface inter500;
    static Typeface inter600;
    static Typeface inter700;
    static Typeface heebo500;
    static Typeface heebo700;

    private Ui() {
    }

    private static AssetManager assets;
    private static volatile boolean settingsFonts;

    /**
     * Loads the app-wide faces once. Thread-safe: the text thread (Doc.loadSync) and the UI thread may both
     * call this; {@code plex} is published last so a caller that sees it non-null sees all five.
     */
    static void fonts(Context context) {
        if (plex != null) {
            return;
        }
        synchronized (Ui.class) {
            if (plex != null) {
                return;
            }
            AssetManager a = context.getApplicationContext() != null ? context.getApplicationContext().getAssets() : context.getAssets();
            assets = a;
            Typeface semi = Typeface.createFromAsset(a, "fonts/plex_semibold.ttf");
            Typeface bold = Typeface.createFromAsset(a, "fonts/plex_bold.ttf");
            Typeface fr = Typeface.createFromAsset(a, "fonts/frank_regular.ttf");
            Typeface frb = Typeface.createFromAsset(a, "fonts/frank_bold.ttf");
            Typeface reg = Typeface.createFromAsset(a, "fonts/plex_regular.ttf");
            plexSemi = semi;
            plexBold = bold;
            frank = fr;
            frankBold = frb;
            plex = reg; // last
        }
    }

    /**
     * Inter / Heebo, only needed by Settings: loaded on first use (or ahead of time on the text thread, see
     * {@link #preloadSettingsFonts}), not on every app start.
     */
    static void settingsFonts() {
        if (settingsFonts) {
            return;
        }
        synchronized (Ui.class) {
            if (settingsFonts || assets == null) {
                return;
            }
            inter500 = Typeface.createFromAsset(assets, "fonts/inter500.ttf");
            inter600 = Typeface.createFromAsset(assets, "fonts/inter600.ttf");
            inter700 = Typeface.createFromAsset(assets, "fonts/inter700.ttf");
            heebo500 = Typeface.createFromAsset(assets, "fonts/heebo500.ttf");
            heebo700 = Typeface.createFromAsset(assets, "fonts/heebo700.ttf");
            settingsFonts = true;
        }
    }

    /** Warms the Settings faces on the text thread so opening Settings doesn't pay for it. */
    static void preloadSettingsFonts() {
        if (!settingsFonts) {
            Doc.runIo(new Runnable() {
                @Override
                public void run() {
                    settingsFonts();
                }
            });
        }
    }

    /** Settings face for text in {@code lang}: weight 500, 600 or 700 (Heebo has 500 / 700 only). */
    static Typeface face(String lang, int weight) {
        settingsFonts();
        if (Str.rtl(lang)) {
            return weight >= 600 ? heebo700 : heebo500;
        }
        return weight >= 700 ? inter700 : weight >= 600 ? inter600 : inter500;
    }

    /** Rounded rectangle with a left-to-right two-colour gradient. */
    static GradientDrawable grad(Context context, int from, int to, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{from, to});
        g.setCornerRadius(dp(context, radiusDp));
        return g;
    }

    static int dp(Context context, float f) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, f, context.getResources().getDisplayMetrics()));
    }

    static TextView text(Context context, CharSequence charSequence, float f, int i, Typeface typeface) {
        return style(new TextView(context), charSequence, f, i, typeface);
    }

    private static TextView style(TextView textView, CharSequence charSequence, float f, int i, Typeface typeface) {
        textView.setText(charSequence);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, f);
        textView.setTextColor(i);
        textView.setTypeface(typeface);
        textView.setIncludeFontPadding(false);
        return textView;
    }

    /** One-line label: "…" at rest, scrolls while its row is focused (see {@link Line}). */
    static Line line(Context context, CharSequence charSequence, float f, int i, Typeface typeface) {
        return (Line) style(new Line(context), charSequence, f, i, typeface);
    }

    static void hebrew(TextView textView) {
        textView.setTextDirection(View.TEXT_DIRECTION_RTL);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
    }

    static void latin(TextView textView) {
        textView.setTextDirection(View.TEXT_DIRECTION_LTR);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    }

    static GradientDrawable round(Context context, int i, float f, int i2, float f2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(context, f));
        if (f2 > 0.0f) {
            gradientDrawable.setStroke(Math.max(1, dp(context, f2)), i2);
        }
        return gradientDrawable;
    }

    static Drawable cardBg(Context context, Pal pal, boolean z) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.setEnterFadeDuration(150);
        stateListDrawable.setExitFadeDuration(150);
        stateListDrawable.addState(new int[]{android.R.attr.state_focused}, round(context, pal.fill, pal.radius, pal.fill, 0.0f));
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, round(context, pal.fill, pal.radius, pal.fill, 0.0f));
        stateListDrawable.addState(new int[0], z ? round(context, pal.card, pal.radius, pal.line, 1.0f) : new ColorDrawable(0));
        return stateListDrawable;
    }

    static LinearLayout.LayoutParams lp(int i, int i2) {
        return new LinearLayout.LayoutParams(i, i2);
    }

    static LinearLayout.LayoutParams lpW(float f) {
        return new LinearLayout.LayoutParams(0, WRAP, f);
    }

    /** Hebrew numeral (gematria) with geresh / gershayim, e.g. 15 -> ט״ו, 5 -> ה׳. */
    static String gem(int n) {
        if (n <= 0) {
            return String.valueOf(n);
        }
        StringBuilder sb = new StringBuilder();
        while (n >= 400) {
            sb.append('ת');
            n -= 400;
        }
        if (n >= 100) {
            sb.append("קרשת".charAt(n / 100 - 1));
            n %= 100;
        }
        // 15 and 16 are written ט״ו / ט״ז so they never spell part of the Name.
        if (n == 15) {
            sb.append("טו");
            n = 0;
        } else if (n == 16) {
            sb.append("טז");
            n = 0;
        }
        if (n >= 10) {
            sb.append("יכלמנסעפצ".charAt(n / 10 - 1));
            n %= 10;
        }
        if (n > 0) {
            sb.append("אבגדהוזחט".charAt(n - 1));
        }
        String s = sb.toString();
        if (s.length() == 1) {
            return s + "׳";
        }
        return s.substring(0, s.length() - 1) + "״" + s.charAt(s.length() - 1);
    }
}
