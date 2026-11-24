package com.pureflip.siddur;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

final class FocusRing extends View {
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.6f);
    private ValueAnimator anim;
    private final RectF cur;
    private final RectF from;
    private boolean has;
    private final Paint paint;
    private float radius;
    private int retries;
    private float stroke;
    private View target;
    private final RectF to;

    View target() {
        return this.target;
    }

    FocusRing(Context context, Pal pal) {
        super(context);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint = paint;
        this.cur = new RectF();
        this.from = new RectF();
        this.to = new RectF();
        this.stroke = Ui.dp(context, 2.5f);
        this.radius = Ui.dp(context, pal.radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(this.stroke);
        paint.setColor(pal.ring);
        setWillNotDraw(false);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(2);
    }

    void setStroke(float dp) {
        this.stroke = Ui.dp(getContext(), dp);
        this.paint.setStrokeWidth(this.stroke);
        invalidate();
    }

    void setRadius(float f) {
        this.radius = Ui.dp(getContext(), f);
        invalidate();
    }

    /** Moves the ring onto {@code rr}; slides from the old spot when {@code animate} and already shown. */
    void moveTo(final View rr, boolean animate) {
        this.target = rr;
        ViewGroup parent = (ViewGroup) getParent();
        if (rr == null) {
            return;
        }
        if (parent == null || rr.getWidth() == 0 || rr.isLayoutRequested()) {
            // Not laid out yet; try again shortly (bounded).
            if (this.retries++ < 8) {
                rr.post(new Runnable() {
                    @Override
                    public void run() {
                        if (target == rr) {
                            moveTo(rr, false);
                        }
                    }
                });
            }
            return;
        }
        this.retries = 0;
        Rect rect = new Rect(0, 0, rr.getWidth(), rr.getHeight());
        try {
            parent.offsetDescendantRectToMyCoords(rr, rect);
        } catch (IllegalArgumentException e) {
            return;
        }
        rect.offset(rr.getScrollX(), rr.getScrollY());
        rect.offset(-getLeft(), -getTop());
        float half = this.stroke / 2.0f;
        this.to.set(rect.left + half, rect.top + half, rect.right - half, rect.bottom - half);
        if (this.anim != null) {
            this.anim.cancel();
        }
        if (this.has && animate) {
            this.from.set(this.cur);
            this.anim = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.anim.setDuration(180L);
            this.anim.setInterpolator(EASE);
            this.anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    float t = (Float) a.getAnimatedValue();
                    cur.set(from.left + (to.left - from.left) * t, from.top + (to.top - from.top) * t,
                            from.right + (to.right - from.right) * t, from.bottom + (to.bottom - from.bottom) * t);
                    invalidate();
                }
            });
            this.anim.start();
            return;
        }
        this.cur.set(this.to);
        this.has = true;
        setAlpha(0.0f);
        animate().alpha(1.0f).setDuration(120L).start();
        invalidate();
    }

    void resnap() {
        View view = this.target;
        if (view != null) {
            boolean z = this.has;
            moveTo(view, false);
            if (z) {
                animate().cancel();
                setAlpha(1.0f);
            }
        }
    }

    void hide() {
        this.has = false;
        this.target = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!this.has) {
            return;
        }
        canvas.drawRoundRect(this.cur, this.radius, this.radius, this.paint);
    }
}
