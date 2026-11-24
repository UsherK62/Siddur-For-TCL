package com.pureflip.siddur;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;

/**
 * A ScrollView that only moves when we tell it to, with one animation curve: scrollY over 180 ms,
 * DecelerateInterpolator(1.6). Built-in "scroll the focused child into view" is switched off so the
 * owner can aim at where a row <i>will</i> be once its expand / collapse animation ends.
 */
final class SmoothScroll extends ScrollView {
    static final long DURATION = 180L;
    private ValueAnimator anim;
    private int target = -1;

    SmoothScroll(Context context) {
        super(context);
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setFocusable(false);
    }

    /** Where the view will rest: the running animation's end, or the current scrollY. */
    int endScrollY() {
        return this.target >= 0 ? this.target : getScrollY();
    }

    void scrollToY(int y, boolean animate) {
        if (this.anim != null) {
            this.anim.cancel();
            this.anim = null;
        }
        y = Math.max(0, y);
        if (!animate || getHeight() == 0) {
            this.target = -1;
            scrollTo(0, y);
            return;
        }
        if (y == getScrollY()) {
            this.target = -1;
            return;
        }
        this.target = y;
        final ValueAnimator a = ValueAnimator.ofInt(getScrollY(), y);
        a.setDuration(DURATION);
        a.setInterpolator(new DecelerateInterpolator(1.6f));
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator v) {
                scrollTo(0, (Integer) v.getAnimatedValue());
            }
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (anim == a) {
                    anim = null;
                    target = -1;
                }
            }
        });
        this.anim = a;
        a.start();
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0; // the owner scrolls (see class comment)
    }
    // Note: ScrollView clamps scrollTo() to the current content height. A row expands in 120 ms and the
    // scroll runs 180 ms, so by the last frames the content has its final height and the target is reached.
}
