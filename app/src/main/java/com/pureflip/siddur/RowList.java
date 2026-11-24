package com.pureflip.siddur;

import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.List;

final class RowList {
    private final Context c;
    final LinearLayout column;
    final FrameLayout frame;
    private final int gap;
    final FocusRing ring;
    final List<View> rows = new ArrayList<>();
    final ScrollView scroll;
    private final boolean stretch;

    ViewGroup view() {
        return this.scroll;
    }

    /**
     * A vertical list of focusable rows with a sliding focus ring.
     *
     * @param stretch rows share the full height (no scrolling) instead of using their own height
     * @param padH    horizontal padding, dp
     * @param padV    vertical padding, dp
     * @param gapDp   gap between rows, dp
     */
    RowList(Context context, Pal pal, boolean stretch, int padH, int padV, int gapDp) {
        this.c = context;
        this.stretch = stretch;
        this.gap = Ui.dp(context, gapDp);
        ScrollView scrollView = new ScrollView(context);
        this.scroll = scrollView;
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setSmoothScrollingEnabled(true);
        scrollView.setFocusable(false);
        FrameLayout frameLayout = new FrameLayout(context);
        this.frame = frameLayout;
        LinearLayout linearLayout = new LinearLayout(context);
        this.column = linearLayout;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(context, padH), Ui.dp(context, padV), Ui.dp(context, padH), Ui.dp(context, padV));
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, stretch ? -1 : -2));
        FocusRing focusRing = new FocusRing(context, pal);
        this.ring = focusRing;
        frameLayout.addView(focusRing, new FrameLayout.LayoutParams(-1, -1));
        scrollView.addView(frameLayout, new FrameLayout.LayoutParams(-1, stretch ? -1 : -2));
        frameLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11) {
                View target = ring.target();
                if (target != null && target.hasFocus()) {
                    ring.resnap();
                }
            }
        });
    }

    void addStatic(View view) {
        this.column.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }

    void addRow(View view, int i, View.OnClickListener onClickListener) {
        LinearLayout.LayoutParams layoutParams;
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnClickListener(onClickListener);
        if (this.stretch) {
            layoutParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        } else {
            layoutParams = new LinearLayout.LayoutParams(-1, i > 0 ? Ui.dp(this.c, i) : -2);
        }
        if (!this.rows.isEmpty() || this.column.getChildCount() > 0) {
            layoutParams.topMargin = this.gap;
        }
        if (i > 0 && this.stretch) {
            view.setMinimumHeight(Ui.dp(this.c, i));
        }
        this.column.addView(view, layoutParams);
        this.rows.add(view);
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view2, boolean z) {
                view2.setSelected(z); // long one-line labels in the focused row scroll (Line)
                if (z) {
                    RowList.this.ring.moveTo(view2, true);
                    RowList.this.reveal(view2);
                }
            }
        });
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view2, int i2, KeyEvent keyEvent) {
                if (keyEvent.getAction() != 0) {
                    return false;
                }
                int indexOf = RowList.this.rows.indexOf(view2);
                if (i2 == 20 && indexOf == RowList.this.rows.size() - 1 && RowList.this.rows.size() > 1 && keyEvent.getRepeatCount() == 0) {
                    RowList.this.focus(0);
                    return true;
                }
                if (i2 == 19 && indexOf == 0 && RowList.this.rows.size() > 1 && keyEvent.getRepeatCount() == 0) {
                    RowList rowList = RowList.this;
                    rowList.focus(rowList.rows.size() - 1);
                    return true;
                }
                if (i2 == 20 && indexOf >= 0 && indexOf < RowList.this.rows.size() - 1) {
                    RowList.this.focus(indexOf + 1);
                    return true;
                }
                if (i2 == 19 && indexOf > 0) {
                    RowList.this.focus(indexOf - 1);
                    return true;
                }
                return false;
            }
        });
    }

    void focus(int i) {
        if (i < 0 || i >= this.rows.size()) {
            return;
        }
        this.rows.get(i).requestFocus();
    }

    /** Scrolls just enough to keep the focused row (plus a small margin) on screen. */
    void reveal(View view) {
        if (this.stretch) {
            return;
        }
        Rect rect = new Rect(0, 0, view.getWidth(), view.getHeight());
        try {
            this.frame.offsetDescendantRectToMyCoords(view, rect);
        } catch (IllegalArgumentException e) {
            return; // view is no longer attached under this list
        }
        int margin = Ui.dp(this.c, 8.0f);
        int scrollY = this.scroll.getScrollY();
        int height = this.scroll.getHeight();
        if (height == 0) {
            return;
        }
        int index = this.rows.indexOf(view);
        if (index == 0) {
            this.scroll.smoothScrollTo(0, 0);
        } else if (index == this.rows.size() - 1) {
            this.scroll.smoothScrollTo(0, this.frame.getHeight());
        } else if (rect.top - margin < scrollY) {
            this.scroll.smoothScrollTo(0, Math.max(0, rect.top - margin));
        } else if (rect.bottom + margin > scrollY + height) {
            this.scroll.smoothScrollTo(0, rect.bottom + margin - height);
        }
    }
}
