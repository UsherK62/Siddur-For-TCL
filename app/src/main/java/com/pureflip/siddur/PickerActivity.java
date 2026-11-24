package com.pureflip.siddur;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PickerActivity extends BaseActivity {
    private String kind;
    private boolean picked;
    private RowList rl;



    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setMenuVisible(false);
        String stringExtra = getIntent().getStringExtra("kind");
        this.kind = stringExtra;
        if (stringExtra == null) {
            this.kind = "layout";
        }
        build();
    }

    private void build() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        String title;
        String sub;
        switch (this.kind) {
            case "nusach":
                title = "nusach";
                sub = "whichNusach";
                break;
            case "city":
                title = "city";
                sub = null;
                break;
            case "lang":
                title = "language";
                sub = null;
                break;
            case "size":
                title = "fontSize";
                sub = "fontSizeSub";
                break;
            case "theme":
                title = "theme";
                sub = "whichTheme";
                break;
            default:
                title = "layout";
                sub = "whichLayout";
                break;
        }
        setHeaderUi(title);
        if (sub != null) {
            TextView line = Ui.line(this, Str.get(this.lang, sub), 13.5f, this.pal.ink2, Ui.plexSemi);
            if (Str.rtl(this.lang)) {
                Ui.hebrew(line);
            } else {
                Ui.latin(line);
            }
            line.setPadding(Ui.dp(this, 10.0f), Ui.dp(this, 5.0f), Ui.dp(this, 10.0f), 0);
            linearLayout.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }
        boolean cards = !"city".equals(this.kind);
        this.rl = new RowList(this, this.pal, cards, 8, 6, cards ? 6 : 3);
        boolean rtl = Str.rtl(this.lang);
        int sel = 0;
        switch (this.kind) {
            case "layout":
                for (int i = 0; i < Prefs.LAYOUTS.length; i++) {
                    final String id = Prefs.LAYOUTS[i];
                    boolean on = id.equals(this.prefs.layout());
                    if (on) {
                        sel = i;
                    }
                    addCard(label(Str.get(this.lang, "l_" + id), 18.0f, on), null, new Runnable() {
                        @Override
                        public void run() {
                            prefs.layout(id);
                        }
                    });
                }
                break;
            case "theme":
                for (int i = 0; i < Pal.ALL.length; i++) {
                    final Pal p = Pal.ALL[i];
                    boolean on = p.id.equals(this.prefs.theme());
                    if (on) {
                        sel = i;
                    }
                    addCard(label(Str.get(this.lang, "t_" + p.id), 19.0f, on), swatches(p), new Runnable() {
                        @Override
                        public void run() {
                            prefs.theme(p.id);
                        }
                    });
                }
                break;
            case "size": {
                String[] keys = {"small", "medium", "large"};
                for (int i = 0; i < 3; i++) {
                    final String id = Prefs.SIZES[i];
                    boolean on = id.equals(this.prefs.size());
                    if (on) {
                        sel = i;
                    }
                    // Sample word at the real reading size.
                    TextView sample = Ui.line(this, "מנחה", Prefs.textSpFor(id) * 1.05f, this.pal.ink, Ui.frankBold);
                    sample.setTextDirection(View.TEXT_DIRECTION_RTL);
                    addCard(label(Str.get(this.lang, keys[i]), 19.0f, on), sample, new Runnable() {
                        @Override
                        public void run() {
                            prefs.size(id);
                        }
                    });
                }
                break;
            }
            case "nusach":
                for (int i = 0; i < 3; i++) {
                    final String id = Prefs.NUSACHOS[i];
                    boolean on = id.equals(this.prefs.nusach());
                    if (on) {
                        sel = i;
                    }
                    addCard(label(rtl ? Names.NUSACH_HE[i] : Names.NUSACH_EN[i], 19.0f, on), null, new Runnable() {
                        @Override
                        public void run() {
                            prefs.nusach(id);
                        }
                    });
                }
                break;
            case "lang": {
                // Prefs.LANGS order: English, Hebrew.
                String[] en = {"English", "Hebrew"};
                String[] own = {"English", "עברית"};
                for (int i = 0; i < Prefs.LANGS.length; i++) {
                    final String id = Prefs.LANGS[i];
                    boolean on = id.equals(this.prefs.lang());
                    if (on) {
                        sel = i;
                    }
                    TextView native_ = label(own[i], 19.0f, on);
                    boolean english = i == 0;
                    TextView latin = ("en".equals(this.lang) && !english) ? label(en[i], 16.0f, on) : null;
                    if (latin != null) {
                        Ui.latin(latin);
                    }
                    if (!english) {
                        Ui.hebrew(native_);
                    }
                    addLangCard(english ? native_ : latin, english ? null : native_, new Runnable() {
                        @Override
                        public void run() {
                            prefs.lang(id);
                        }
                    });
                }
                break;
            }
            case "city":
                for (int i = 0; i < Cities.ALL.length; i++) {
                    final Cities.City c = Cities.ALL[i];
                    boolean on = c.id.equals(this.prefs.city());
                    if (on) {
                        sel = i;
                    }
                    addCard(label(c.name(this.lang), 14.0f, on), null, 27, new Runnable() {
                        @Override
                        public void run() {
                            prefs.city(c.id);
                        }
                    });
                }
                break;
        }
        final int f = sel;
        linearLayout.addView(this.rl.view(), new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.body.addView(linearLayout, fill());
        this.rl.scroll.post(new Runnable() {
            @Override
            public void run() {
                rl.focus(f);
            }
        });
    }

    private TextView label(String str, float f, boolean z) {
        Pal pal = this.pal;
        TextView line = Ui.line(this, str, f, z ? pal.acc : pal.ink, z ? Ui.plexBold : Ui.plexSemi);
        if (Str.rtl(this.lang)) {
            Ui.hebrew(line);
        } else {
            Ui.latin(line);
        }
        return line;
    }

    private LinearLayout swatches(Pal pal) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < pal.swatches.length; i++) {
            View view = new View(this);
            view.setBackground(Ui.round(this, pal.swatches[i], 4.0f, this.pal.dark ? 1728053247 : 1426063360, 1.0f));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(this, 17.0f), Ui.dp(this, 17.0f));
            if (i > 0) {
                layoutParams.leftMargin = Ui.dp(this, 3.0f);
            }
            linearLayout.addView(view, layoutParams);
        }
        return linearLayout;
    }

    private void addCard(TextView textView, View view, Runnable runnable) {
        addCard(textView, view, 40, runnable);
    }

    private void addCard(TextView textView, View view, int i, Runnable runnable) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 14.0f), 0, Ui.dp(this, 14.0f), 0);
        linearLayout.setBackground(Ui.cardBg(this, this.pal, true));
        boolean rtl = Str.rtl(this.lang);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        if (rtl) {
            if (view != null) {
                linearLayout.addView(view);
            }
            linearLayout.addView(textView, layoutParams);
        } else {
            linearLayout.addView(textView, layoutParams);
            if (view != null) {
                linearLayout.addView(view);
            }
        }
        this.rl.addRow(linearLayout, i, click(runnable));
    }

    private void addLangCard(TextView textView, TextView textView2, Runnable runnable) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 14.0f), 0, Ui.dp(this, 14.0f), 0);
        linearLayout.setBackground(Ui.cardBg(this, this.pal, true));
        if (textView != null) {
            Ui.latin(textView);
            linearLayout.addView(textView, new LinearLayout.LayoutParams(0, -2, 1.0f));
        } else {
            linearLayout.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
        }
        if (textView2 != null) {
            linearLayout.addView(textView2, new LinearLayout.LayoutParams(-2, -2));
        }
        this.rl.addRow(linearLayout, 40, click(runnable));
    }

    /** Pick once: run the action, give a small press bounce, then go back. */
    private View.OnClickListener click(final Runnable action) {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (picked) {
                    return;
                }
                picked = true;
                action.run();
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90L).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                onBackPressed();
                            }
                        }).start();
                    }
                }).start();
            }
        };
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9 && this.rl != null && !"city".equals(this.kind)) {
            int idx = keyCode - KeyEvent.KEYCODE_1;
            if (idx < this.rl.rows.size()) {
                this.rl.rows.get(idx).performClick();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}
