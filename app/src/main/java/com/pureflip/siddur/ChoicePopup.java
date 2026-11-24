package com.pureflip.siddur;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings picker, Auto Reply style: a centred rounded card on a scrim with one radio row per option.
 * Fades / scales in over 150 ms and out over 120 ms. The soft-key bar stays visible below the scrim.
 * Up / Down move (wrapping), OK picks, Back / Menu close, 1-9 pick directly.
 */
final class ChoicePopup {
    interface OnPick {
        void pick(int index);
    }

    static final long IN_MS = 150L;
    static final long OUT_MS = 120L;
    private static final float ROW_DP = 27.0f;
    private static final float GAP_DP = 3.0f;

    private final Context c;
    private final FrameLayout host;
    private final Pal p;
    private final String lang;
    private final FrameLayout scrim;
    private final LinearLayout card;
    private final SmoothScroll scroll;
    private final LinearLayout column;
    final List<OptionRow> rows = new ArrayList<>();
    private final int barPx;
    private OnPick onPick;
    private Runnable onClose;
    private int checked;
    private boolean showing;
    private boolean picking;

    ChoicePopup(Context c, FrameLayout host, Pal p, String lang, String title, int barPx) {
        this.c = c;
        this.host = host;
        this.p = p;
        this.lang = lang;
        this.barPx = barPx;
        this.scrim = new FrameLayout(c);
        this.scrim.setBackgroundColor(p.scrim);
        this.scrim.setClickable(true);
        this.scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(null);
            }
        });
        this.card = new LinearLayout(c);
        this.card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(p.pop);
        bg.setCornerRadius(Ui.dp(c, 14.0f));
        this.card.setBackground(bg);
        this.card.setElevation(Ui.dp(c, 12.0f));
        this.card.setClickable(true);
        int padH = Ui.dp(c, 6.0f);
        this.card.setPadding(padH, Ui.dp(c, 10.0f), padH, Ui.dp(c, 10.0f));
        TextView t = Ui.line(c, title, 15.0f, p.ink, Ui.face(lang, 700));
        if (Str.rtl(lang)) {
            Ui.hebrew(t);
        } else {
            Ui.latin(t);
        }
        t.setPadding(Ui.dp(c, 7.0f), 0, Ui.dp(c, 7.0f), 0);
        this.card.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 22.0f)));
        this.scroll = new SmoothScroll(c);
        this.column = new LinearLayout(c);
        this.column.setOrientation(LinearLayout.VERTICAL);
        this.scroll.addView(this.column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.card.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        int m = Ui.dp(c, 14.0f);
        lp.leftMargin = m;
        lp.rightMargin = m;
        lp.topMargin = Ui.dp(c, 10.0f);
        lp.bottomMargin = Ui.dp(c, 10.0f);
        this.scrim.addView(this.card, lp);
    }

    boolean isShowing() {
        return this.showing;
    }

    int checked() {
        return this.checked;
    }

    ChoicePopup onPick(OnPick r) {
        this.onPick = r;
        return this;
    }

    ChoicePopup onClose(Runnable r) {
        this.onClose = r;
        return this;
    }

    /** Adds an option; {@code dots} (theme colours) go on the far side, may be null. */
    ChoicePopup option(String text, int[] dots) {
        final int index = this.rows.size();
        OptionRow row = new OptionRow(this.c, this.p, this.lang, text, dots);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this.c, ROW_DP));
        if (index > 0) {
            lp.topMargin = Ui.dp(this.c, GAP_DP);
        }
        this.column.addView(row, lp);
        this.rows.add(row);
        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                if (has) {
                    reveal(v, true);
                }
            }
        });
        row.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int code, KeyEvent e) {
                if (e.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                int n = rows.size();
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focus(index + 1 < n ? index + 1 : (e.getRepeatCount() == 0 ? 0 : index));
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_UP) {
                    focus(index > 0 ? index - 1 : (e.getRepeatCount() == 0 ? n - 1 : index));
                    return true;
                }
                return false;
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(index);
            }
        });
        return this;
    }

    void focus(int i) {
        if (i >= 0 && i < this.rows.size()) {
            this.rows.get(i).requestFocus();
        }
    }

    void pick(final int index) {
        if (this.picking || !this.showing) {
            return;
        }
        this.picking = true;
        setChecked(index);
        this.card.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss(new Runnable() {
                    @Override
                    public void run() {
                        if (onPick != null) {
                            onPick.pick(index);
                        }
                    }
                });
            }
        }, 90L);
    }

    void setChecked(int index) {
        this.checked = index;
        for (int i = 0; i < this.rows.size(); i++) {
            this.rows.get(i).setChecked(i == index);
        }
    }

    private void reveal(View row, boolean animate) {
        int h = this.scroll.getHeight();
        if (h == 0) {
            return;
        }
        int top = row.getTop();
        int bottom = top + row.getHeight();
        int m = Ui.dp(this.c, 4.0f);
        int cur = this.scroll.endScrollY();
        int to = cur;
        if (top - m < cur) {
            to = top - m;
        } else if (bottom + m > cur + h) {
            to = bottom + m - h;
        }
        int max = Math.max(0, this.column.getHeight() - h);
        to = Math.max(0, Math.min(max, to));
        if (to != cur) {
            this.scroll.scrollToY(to, animate);
        }
    }

    void show(int checkedIndex) {
        if (this.showing) {
            return;
        }
        this.showing = true;
        setChecked(checkedIndex);
        // Cap the list so the card fits between the title strip and the soft-key bar.
        int hostH = this.host.getHeight() > 0 ? this.host.getHeight() : this.c.getResources().getDisplayMetrics().heightPixels;
        int avail = hostH - this.barPx - Ui.dp(this.c, 20.0f + 20.0f + 22.0f + 8.0f);
        int n = this.rows.size();
        int want = n * Ui.dp(this.c, ROW_DP) + Math.max(0, n - 1) * Ui.dp(this.c, GAP_DP);
        this.scroll.getLayoutParams().height = Math.min(want, Math.max(Ui.dp(this.c, ROW_DP) * 3, avail));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.bottomMargin = this.barPx;
        this.host.addView(this.scrim, lp);
        this.scrim.setAlpha(0.0f);
        this.scrim.animate().alpha(1.0f).setDuration(IN_MS).start();
        this.card.setScaleX(0.94f);
        this.card.setScaleY(0.94f);
        this.card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(IN_MS).start();
        final int fi = Math.max(0, Math.min(checkedIndex, n - 1));
        this.scrim.post(new Runnable() {
            @Override
            public void run() {
                if (fi < rows.size()) {
                    OptionRow r = rows.get(fi);
                    reveal(r, false);
                    r.requestFocus();
                }
            }
        });
    }

    void dismiss(final Runnable then) {
        if (!this.showing) {
            return;
        }
        this.showing = false;
        this.card.animate().scaleX(0.96f).scaleY(0.96f).setDuration(OUT_MS).start();
        this.scrim.animate().alpha(0.0f).setDuration(OUT_MS).withEndAction(new Runnable() {
            @Override
            public void run() {
                host.removeView(scrim);
                if (c instanceof android.app.Activity && ((android.app.Activity) c).isDestroyed()) {
                    return; // screen went away during the fade (recreate / rotation)
                }
                if (onClose != null) {
                    onClose.run();
                }
                if (then != null) {
                    then.run();
                }
            }
        }).start();
    }

    /** Keys while showing. Returns true if consumed. */
    boolean dispatch(KeyEvent e) {
        int code = e.getKeyCode();
        if (BaseActivity.isBack(code) || BaseActivity.isMenu(code)) {
            if (e.getAction() == KeyEvent.ACTION_UP) {
                dismiss(null);
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return true;
        }
        if (code >= KeyEvent.KEYCODE_1 && code <= KeyEvent.KEYCODE_9) {
            int i = code - KeyEvent.KEYCODE_1;
            if (e.getAction() == KeyEvent.ACTION_UP && i < this.rows.size()) {
                pick(i);
            }
            return true;
        }
        if (this.card.findFocus() == null && e.getAction() == KeyEvent.ACTION_DOWN) {
            focus(this.checked);
            return true;
        }
        return false;
    }
}
