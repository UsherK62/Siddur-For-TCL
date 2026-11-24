package com.pureflip.siddur;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

abstract class BaseActivity extends Activity {
    protected TextView barLeft;
    protected TextView barOk;
    protected TextView barRight;
    protected FrameLayout body;
    private int gen;
    protected TextView headLeft;
    protected TextView headRight;
    protected LinearLayout header;
    protected String lang;
    protected Pal pal;
    private TextView pill;
    protected Popup popup;
    protected Prefs prefs;
    protected LinearLayout root;
    protected FrameLayout top;

    static boolean isBack(int i) {
        return i == 2 || i == 132 || i == 4;
    }

    static boolean isMenu(int i) {
        return i == 1 || i == 82 || i == 131;
    }

    static boolean isOk(int i) {
        return i == 23 || i == 66 || i == 160;
    }

    protected void onMenuKey() {
    }

    protected boolean onOkKey() {
        return false;
    }

    BaseActivity() {
    }

    @Override
    protected void attachBaseContext(Context context) {
        int i;
        super.attachBaseContext(context);
        Configuration configuration = new Configuration();
        configuration.fontScale = 1.0f;
        if (Build.VERSION.SDK_INT >= 24) {
            i = DisplayMetrics.DENSITY_DEVICE_STABLE;
            int i2 = context.getResources().getConfiguration().densityDpi;
            if (i > 0 && i2 > i) {
                configuration.densityDpi = i;
            }
        }
        applyOverrideConfiguration(configuration);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        TclMenuBar.beforeContent(this);   // no TCL "dead" soft-key strip over our bar (see TclMenuBar)
        super.onCreate(bundle);
        Prefs prefs = new Prefs(this);
        this.prefs = prefs;
        this.pal = Pal.of(prefs.theme());
        this.lang = this.prefs.lang();
        this.gen = this.prefs.styleGen();
        Ui.fonts(this);
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(this.pal.bg));
        window.addFlags(Integer.MIN_VALUE);
        window.setStatusBarColor(this.pal.status);
        window.setNavigationBarColor(this.pal.bar);
        this.top = new FrameLayout(this);
        LinearLayout linearLayout = new LinearLayout(this);
        this.root = linearLayout;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        this.top.addView(this.root, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayout2 = new LinearLayout(this);
        this.header = linearLayout2;
        linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
        this.header.setGravity(Gravity.CENTER_VERTICAL);
        this.header.setPadding(Ui.dp(this, 9.0f), 0, Ui.dp(this, 9.0f), 0);
        this.header.setBackgroundColor(this.pal.headerBg);
        TextView line = Ui.line(this, "", 15.0f, this.pal.headerFg, Ui.plexBold);
        this.headLeft = line;
        Ui.latin(line);
        TextView line2 = Ui.line(this, "", 15.0f, this.pal.headerFg, Ui.plexSemi);
        this.headRight = line2;
        Ui.hebrew(line2);
        this.header.addView(this.headLeft, new LinearLayout.LayoutParams(-2, -1));
        this.header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
        this.header.addView(this.headRight, new LinearLayout.LayoutParams(-2, -1));
        this.root.addView(this.header, new LinearLayout.LayoutParams(-1, Ui.dp(this, 27.0f)));
        if (this.pal.lightBars) {
            View view = new View(this);
            view.setBackgroundColor(this.pal.barLine);
            this.root.addView(view, new LinearLayout.LayoutParams(-1, 1));
        }
        FrameLayout frameLayout = new FrameLayout(this);
        this.body = frameLayout;
        this.root.addView(frameLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        // (No line above the soft-key bar: the bar is dark in every theme, Auto Reply style.)
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout3.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout3.setPadding(Ui.dp(this, 8.0f), 0, Ui.dp(this, 8.0f), 0);
        linearLayout3.setBackgroundColor(this.pal.bar);
        TextView line3 = Ui.line(this, Str.get(this.lang, "menu"), 12.5f, this.pal.barFg, Ui.plexSemi);
        this.barLeft = line3;
        Ui.latin(line3);
        if (Str.rtl(this.lang)) {
            this.barLeft.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        this.barLeft.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        TextView line4 = Ui.line(this, Str.get(this.lang, "back"), 12.5f, this.pal.barFg, Ui.plexSemi);
        this.barRight = line4;
        line4.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        TextView line5 = Ui.line(this, Str.get(this.lang, "ok"), 12.0f, this.pal.pillFg, Ui.plexBold);
        this.barOk = line5;
        line5.setGravity(Gravity.CENTER);
        this.barOk.setBackground(Ui.grad(this, this.pal.acc, this.pal.acc2, 9.0f));
        linearLayout3.addView(this.barLeft, new LinearLayout.LayoutParams(0, -1, 1.0f));
        linearLayout3.addView(this.barOk, new LinearLayout.LayoutParams(Ui.dp(this, 44.0f), Ui.dp(this, 15.0f)));
        linearLayout3.addView(this.barRight, new LinearLayout.LayoutParams(0, -1, 1.0f));
        this.root.addView(linearLayout3, new LinearLayout.LayoutParams(-1, Ui.dp(this, 21.0f)));
        setupTouch(linearLayout3);
        setContentView(this.top);
        TclMenuBar.afterContent(this);
    }

    protected void setHeader(String str, String str2) {
        this.headLeft.setText(str == null ? "" : str);
        this.headLeft.setVisibility((str == null || str.isEmpty()) ? View.GONE : View.VISIBLE);
        TextView textView = this.headRight;
        if (str2 == null) {
            str2 = "";
        }
        textView.setText(str2);
    }

    protected void setHeaderUi(String str) {
        String str2 = Str.get(this.lang, str);
        if (Str.rtl(this.lang)) {
            setHeader(null, str2);
        } else {
            setHeader(str2, null);
        }
    }

    protected View focused() {
        return getWindow().getDecorView().findFocus();
    }

    protected void setMenuVisible(boolean z) {
        this.barLeft.setVisibility(z ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.prefs.styleGen() != this.gen) {
            recreate();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        Popup popup = this.popup;
        if (popup != null && popup.isShowing()) {
            if (isOk(keyCode) && keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                press(this.barOk);
            }
            if (this.popup.dispatch(keyEvent)) {
                return true;
            }
            return super.dispatchKeyEvent(keyEvent);
        }
        if (isMenu(keyCode)) {
            if (keyEvent.getAction() == 1) {
                press(this.barLeft);
                onMenuKey();
            }
            return true;
        }
        if (isBack(keyCode)) {
            if (keyEvent.getAction() == 1 && !keyEvent.isCanceled()) {
                press(this.barRight);
                onBackPressed();
            }
            return true;
        }
        if (isOk(keyCode) && keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
            press(this.barOk);
            if (onOkKey()) {
                return true;
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    protected void press(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140L).start();
    }

    private void setupTouch(LinearLayout linearLayout) {
        this.barLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaseActivity.this.onMenuKey();
            }
        });
        this.barRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaseActivity.this.onBackPressed();
            }
        });
        this.barOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focused;
                if (BaseActivity.this.onOkKey() || (focused = BaseActivity.this.focused()) == null) {
                    return;
                }
                focused.performClick();
            }
        });
        this.barLeft.setFocusable(false);
        this.barRight.setFocusable(false);
        this.barOk.setFocusable(false);
    }

    @Override
    public void onBackPressed() {
        Popup popup = this.popup;
        if (popup != null && popup.isShowing()) {
            this.popup.dismiss();
        } else {
            finish();
            overridePendingTransition(R.anim.back_enter, R.anim.back_exit);
        }
    }

    protected void go(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.fwd_enter, R.anim.fwd_exit);
    }

    protected void go(Class<?> cls) {
        go(new Intent(this, cls));
    }

    protected Popup newPopup(String str) {
        Popup popup = this.popup;
        if (popup != null && popup.isShowing()) {
            this.popup.dismiss();
        }
        final Popup popup2 = new Popup(this, this.top, this.pal, this.lang, str);
        this.popup = popup2;
        // Like the Settings picker: scrim stops above the soft-key bar, Back reads "Cancel" while open.
        View bar = this.barOk != null ? (View) this.barOk.getParent() : null;
        popup2.bar(bar != null && bar.getHeight() > 0 ? bar.getHeight() : Ui.dp(this, 21.0f));
        if (this.barRight != null) {
            final CharSequence was = this.barRight.getText();
            this.barRight.setText(Str.get(this.lang, "cancel"));
            popup2.onClose(new Runnable() {
                @Override
                public void run() {
                    if (popup == popup2 || popup == null || !popup.isShowing()) {
                        barRight.setText(was);
                    }
                }
            });
        }
        return popup2;
    }

    protected void flash(String str) {
        if (this.pill == null) {
            TextView line = Ui.line(this, "", 13.0f, this.pal.pillFg, Ui.plexSemi);
            this.pill = line;
            line.setGravity(Gravity.CENTER);
            this.pill.setPadding(Ui.dp(this, 12.0f), Ui.dp(this, 4.0f), Ui.dp(this, 12.0f), Ui.dp(this, 5.0f));
            this.pill.setBackground(Ui.round(this, this.pal.pill, 12.0f, this.pal.pill, 0.0f));
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 81);
            layoutParams.bottomMargin = Ui.dp(this, 30.0f);
            this.top.addView(this.pill, layoutParams);
        }
        if (Str.rtl(this.lang)) {
            this.pill.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        this.pill.setText(str);
        this.pill.animate().cancel();
        this.pill.setAlpha(0.0f);
        this.pill.setTranslationY(Ui.dp(this, 6.0f));
        this.pill.setVisibility(View.VISIBLE);
        this.pill.animate().alpha(1.0f).translationY(0.0f).setDuration(180L).withEndAction(new Runnable() {
            @Override
            public void run() {
                BaseActivity.this.pill.animate().alpha(0.0f).setStartDelay(1100L).setDuration(220L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        BaseActivity.this.pill.setVisibility(View.GONE);
                        BaseActivity.this.pill.animate().setStartDelay(0L);
                    }
                }).start();
            }
        }).start();
    }

    protected LinearLayout twoSide(CharSequence charSequence, CharSequence charSequence2, float f, int i, int i2, Typeface typeface, Typeface typeface2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        if (charSequence != null) {
            TextView line = Ui.line(this, charSequence, f, i, typeface);
            Ui.latin(line);
            linearLayout.addView(line, new LinearLayout.LayoutParams(0, -2, 1.0f));
            if (charSequence2 != null) {
                shrinkToFit(line, 0.7f * f);
            }
        } else {
            linearLayout.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
        }
        if (charSequence2 != null) {
            TextView line2 = Ui.line(this, charSequence2, f, i2, typeface2);
            Ui.hebrew(line2);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(charSequence != null ? -2 : 0, -2, charSequence != null ? 0.0f : 1.0f);
            layoutParams.leftMargin = Ui.dp(this, 6.0f);
            if (charSequence == null) {
                linearLayout.removeAllViews();
            }
            linearLayout.addView(line2, layoutParams);
        }
        return linearLayout;
    }

    static void shrinkToFit(final TextView t, final float minSp) {
        t.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int avail = t.getWidth() - t.getCompoundPaddingLeft() - t.getCompoundPaddingRight();
                if (avail <= 0) {
                    return;
                }
                float density = t.getResources().getDisplayMetrics().scaledDensity;
                float sp = t.getTextSize() / density;
                TextPaint paint = new TextPaint(t.getPaint());
                String s = t.getText().toString();
                paint.setTextSize(sp * density);
                float size = sp;
                while (paint.measureText(s) > avail - 1) {
                    float smaller = size - 0.5f;
                    if (smaller < minSp) {
                        break;
                    }
                    paint.setTextSize(smaller * density);
                    size = smaller;
                }
                if (size < sp) {
                    final float next = size;
                    t.post(new Runnable() {
                        @Override
                        public void run() {
                            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, next);
                        }
                    });
                }
            }
        });
    }

    protected static void fadeIn(View view) {
        view.setAlpha(0.0f);
        view.animate().alpha(1.0f).setDuration(200L).start();
    }

    protected static ViewGroup.LayoutParams fill() {
        return new FrameLayout.LayoutParams(-1, -1);
    }
}
