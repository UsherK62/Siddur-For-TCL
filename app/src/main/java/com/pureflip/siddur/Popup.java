package com.pureflip.siddur;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Popup {
    private final Context c;
    private final LinearLayout card;
    private final FrameLayout dim;
    private final FrameLayout host;
    private final String lang;
    private final RowList list;
    private final Pal p;
    private boolean showing;
    private Runnable onClose;
    private int barPx;
    private View before;
    /** Row that is already in effect (e.g. today's text size): OK on it just closes. -1 if none. */
    private int current = -1;
    private boolean okDown;

    interface OnPick {
        void pick(int index);
    }

    boolean isShowing() {
        return this.showing;
    }

    Popup(Context context, FrameLayout frameLayout, Pal pal, String str, String str2) {
        this.c = context;
        this.host = frameLayout;
        this.p = pal;
        this.lang = str;
        FrameLayout frameLayout2 = new FrameLayout(context);
        this.dim = frameLayout2;
        frameLayout2.setBackgroundColor(pal.scrim);
        frameLayout2.setClickable(true);
        frameLayout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        LinearLayout linearLayout = new LinearLayout(context);
        this.card = linearLayout;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        // Same card as the Settings picker (ChoicePopup): radius 14, elevation 12, no border, 15 sp bold title.
        linearLayout.setBackground(Ui.round(context, pal.pop, 14.0f, pal.pop, 0.0f));
        linearLayout.setElevation(Ui.dp(context, 12.0f));
        linearLayout.setClickable(true);
        linearLayout.setPadding(0, Ui.dp(context, 8.0f), 0, Ui.dp(context, 8.0f));
        if (str2 != null) {
            TextView line = Ui.line(context, str2, 15.0f, pal.ink, Ui.face(str, 700));
            if (Str.rtl(str)) {
                Ui.hebrew(line);
            } else {
                Ui.latin(line);
            }
            line.setPadding(Ui.dp(context, 12.0f), Ui.dp(context, 2.0f), Ui.dp(context, 12.0f), Ui.dp(context, 4.0f));
            linearLayout.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }
        RowList rowList = new RowList(context, pal, false, 6, 2, 3);
        rowList.ring.setStroke(1.5f);
        rowList.ring.setRadius(9.0f);
        this.list = rowList;
        linearLayout.addView(rowList.view(), new LinearLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2, 17);
        int dp = Ui.dp(context, 14.0f);
        layoutParams.rightMargin = dp;
        layoutParams.leftMargin = dp;
        layoutParams.topMargin = Ui.dp(context, 10.0f);
        layoutParams.bottomMargin = Ui.dp(context, 10.0f);
        frameLayout2.addView(linearLayout, layoutParams);
    }

    Popup item(String en, String he, Runnable r) {
        return item(en, he, 14.0f, 0, r);
    }

    /** Adds a row; {@code endPad} is extra right padding in dp (also dims the Hebrew label). */
    Popup item(String str, String str2, float f, int endPad, final Runnable r) {
        final int index = this.list.rows.size();
        LinearLayout linearLayout = new LinearLayout(this.c);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setPadding(Ui.dp(this.c, 10.0f), 0, Ui.dp(this.c, endPad + 10), 0);
        linearLayout.setBackground(Ui.cardBg(this.c, this.p, false));
        if (str != null) {
            TextView line = Ui.line(this.c, str, f, this.p.ink, Ui.plexSemi);
            Ui.latin(line);
            linearLayout.addView(line, new LinearLayout.LayoutParams(0, -2, 1.0f));
        }
        if (str2 != null) {
            TextView line2 = Ui.line(this.c, str2, f, endPad > 0 ? this.p.ink2 : this.p.ink, Ui.plexSemi);
            Ui.hebrew(line2);
            linearLayout.addView(line2, new LinearLayout.LayoutParams(str != null ? -2 : 0, -2, str != null ? 0.0f : 1.0f));
        }
        this.list.addRow(linearLayout, f < 14.0f ? 28 : 32, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!showing) {
                    return; // a second OK during the 120 ms fade-out must not run the action twice
                }
                dismiss();
                if (r != null && index != current) {
                    r.run();
                }
            }
        });
        return this;
    }

    /** One UI-language label, placed on the side that matches the language. */
    Popup ui(String label, Runnable r) {
        return Str.rtl(this.lang) ? item(null, label, r) : item(label, null, r);
    }

    /** Soft-key bar height: the scrim stops above it so the bar stays visible (as in Settings). */
    Popup bar(int px) {
        this.barPx = px;
        return this;
    }

    /** Marks row {@code i} as the one already in effect: OK there closes without running it again. */
    Popup current(int i) {
        this.current = i;
        return this;
    }

    Popup onClose(Runnable r) {
        this.onClose = r;
        return this;
    }

    void show(final int fi) {
        if (this.showing) {
            return;
        }
        this.showing = true;
        this.okDown = false;
        this.before = this.host.findFocus();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
        lp.bottomMargin = this.barPx;
        this.host.addView(this.dim, lp);
        this.dim.setAlpha(0.0f);
        this.dim.animate().alpha(1.0f).setDuration(ChoicePopup.IN_MS).start();
        this.card.setScaleX(0.94f);
        this.card.setScaleY(0.94f);
        this.card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(ChoicePopup.IN_MS).start();
        this.dim.post(new Runnable() {
            @Override
            public void run() {
                if (showing) {
                    list.focus(Math.max(0, Math.min(fi, list.rows.size() - 1)));
                }
            }
        });
    }

    void dismiss() {
        if (!this.showing) {
            return;
        }
        this.showing = false;
        // Let go of focus right away so keys during the fade go back to the screen, not to a dying row.
        this.card.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        View f = this.card.findFocus();
        View back = this.before;
        this.before = null;
        if (back != null && back.isAttachedToWindow() && back.isFocusable() && back.getVisibility() == View.VISIBLE) {
            back.requestFocus();
        } else if (f != null) {
            f.clearFocus();
        }
        this.card.animate().scaleX(0.96f).scaleY(0.96f).setDuration(ChoicePopup.OUT_MS).start();
        this.dim.animate().alpha(0.0f).setDuration(ChoicePopup.OUT_MS).withEndAction(new Runnable() {
            @Override
            public void run() {
                host.removeView(dim);
            }
        }).start();
        if (this.onClose != null) {
            this.onClose.run();
        }
    }

    boolean dispatch(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (BaseActivity.isBack(keyCode) || BaseActivity.isMenu(keyCode)) {
            if (keyEvent.getAction() == 1) {
                dismiss();
            }
            return true;
        }
        if (keyCode == 21 || keyCode == 22) {
            return true;
        }
        if (BaseActivity.isOk(keyCode)) {
            // OK runs the focused row on release (its own click closes the popup). It never runs a row it
            // didn't land on: with nothing focused it just closes. Only a press begun while open counts.
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyEvent.getRepeatCount() == 0) {
                    this.okDown = true;
                }
            } else if (keyEvent.getAction() == KeyEvent.ACTION_UP && this.okDown) {
                this.okDown = false;
                int i = this.list.rows.indexOf(this.card.findFocus());
                if (i >= 0) {
                    this.list.rows.get(i).performClick();
                } else {
                    dismiss();
                }
            }
            return true;
        }
        if (keyCode >= 8 && keyCode <= 16) {
            int i = keyCode - 8;
            if (keyEvent.getAction() == 1 && i < this.list.rows.size()) {
                this.list.rows.get(i).performClick();
            }
            return true;
        }
        if (this.card.findFocus() == null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            this.list.focus(0); // nothing focused yet: first D-pad press lands on the first row
            return true;
        }
        return false;
    }
}
