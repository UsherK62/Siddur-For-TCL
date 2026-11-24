package com.pureflip.siddur;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Small drawn marks used by Settings: on/off switch, radio mark, chevron, theme swatches. */
final class Marks {
    private Marks() {
    }

    /** On/off switch, 34 x 20 dp; the knob slides over 150 ms. */
    static final class Toggle extends View {
        private final Pal p;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private float pos;
        private boolean on;
        private ValueAnimator anim;
        private LinearGradient shader;
        private int shaderW = -1;
        private final float knobR;
        private final float knobDrop;

        Toggle(Context c, Pal p, boolean on) {
            super(c);
            this.p = p;
            this.on = on;
            this.pos = on ? 1.0f : 0.0f;
            this.knobR = Ui.dp(c, 7.0f);
            this.knobDrop = Ui.dp(c, 0.75f);
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        boolean isOn() {
            return this.on;
        }

        void setOn(boolean v, boolean animate) {
            this.on = v;
            if (this.anim != null) {
                this.anim.cancel();
            }
            float to = v ? 1.0f : 0.0f;
            if (!animate) {
                this.pos = to;
                invalidate();
                return;
            }
            this.anim = ValueAnimator.ofFloat(this.pos, to);
            this.anim.setDuration(150L);
            this.anim.setInterpolator(new DecelerateInterpolator(1.6f));
            this.anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    pos = (Float) a.getAnimatedValue();
                    invalidate();
                }
            });
            this.anim.start();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(Ui.dp(getContext(), 34.0f), Ui.dp(getContext(), 20.0f));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float rad = h / 2.0f;
            this.r.set(0, 0, w, h);
            this.paint.setShader(null);
            this.paint.setColor(this.p.off);
            c.drawRoundRect(this.r, rad, rad, this.paint);
            if (this.pos > 0.0f) {
                if (this.shader == null || this.shaderW != getWidth()) {
                    this.shader = new LinearGradient(0, 0, w, 0, this.p.acc, this.p.acc2, Shader.TileMode.CLAMP);
                    this.shaderW = getWidth();
                }
                this.paint.setShader(this.shader);
                this.paint.setAlpha(Math.round(255 * this.pos));
                c.drawRoundRect(this.r, rad, rad, this.paint);
                this.paint.setShader(null);
                this.paint.setAlpha(255);
            }
            float knob = this.knobR;
            float cx = rad + (w - 2 * rad) * this.pos;
            this.paint.setColor(0x33000000);
            c.drawCircle(cx, h / 2.0f + this.knobDrop, knob, this.paint);
            this.paint.setColor(0xFFFFFFFF);
            c.drawCircle(cx, h / 2.0f, knob, this.paint);
        }
    }

    /** Radio mark, 16 dp: accent gradient disc with a dot when chosen, a thin ring when not. */
    static final class Radio extends View {
        private final Pal p;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean on;
        private LinearGradient shader;
        private int shaderW = -1;

        Radio(Context c, Pal p, boolean on) {
            super(c);
            this.p = p;
            this.on = on;
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        boolean isOn() {
            return this.on;
        }

        void setOn(boolean v) {
            this.on = v;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            int s = Ui.dp(getContext(), 16.0f);
            setMeasuredDimension(s, s);
        }

        @Override
        protected void onDraw(Canvas c) {
            float s = getWidth();
            float cx = s / 2.0f;
            if (this.on) {
                this.paint.setStyle(Paint.Style.FILL);
                if (this.shader == null || this.shaderW != getWidth()) {
                    this.shader = new LinearGradient(0, 0, s, 0, this.p.acc, this.p.acc2, Shader.TileMode.CLAMP);
                    this.shaderW = getWidth();
                }
                this.paint.setShader(this.shader);
                c.drawCircle(cx, cx, cx, this.paint);
                this.paint.setShader(null);
                this.paint.setColor(this.p.dark ? this.p.pillFg : 0xFFFFFFFF);
                c.drawCircle(cx, cx, s * 0.19f, this.paint);
            } else {
                float sw = Ui.dp(getContext(), 1.6f);
                this.paint.setShader(null);
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeWidth(sw);
                this.paint.setColor(this.p.off);
                c.drawCircle(cx, cx, cx - sw / 2.0f, this.paint);
            }
        }
    }

    /** "Opens a screen" chevron; points left in RTL (forward), right in LTR. */
    static final class Chevron extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean rtl;
        private final Path path = new Path();

        Chevron(Context c, int color, boolean rtl) {
            super(c);
            this.rtl = rtl;
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(Ui.dp(c, 1.4f));
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            this.paint.setColor(color);
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(Ui.dp(getContext(), 8.0f), Ui.dp(getContext(), 10.0f));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float a = Ui.dp(getContext(), 2.0f);
            Path path = this.path;
            path.rewind();
            if (this.rtl) {
                path.moveTo(w - a, a);
                path.lineTo(a, h / 2.0f);
                path.lineTo(w - a, h - a);
            } else {
                path.moveTo(a, a);
                path.lineTo(w - a, h / 2.0f);
                path.lineTo(a, h - a);
            }
            c.drawPath(path, this.paint);
        }
    }

    /** A theme's colour dots (8 dp each, 2 dp apart) with a hairline edge so white / black dots show. */
    static final class Swatches extends View {
        private final int[] colors;
        private final int edge;
        private final float d;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Swatches(Context c, int[] colors, int edge, float dotDp) {
            super(c);
            this.colors = colors;
            this.edge = edge;
            this.d = Ui.dp(c, dotDp);
        }

        @Override
        protected void onMeasure(int w, int h) {
            float gap = Ui.dp(getContext(), 2.0f);
            setMeasuredDimension(Math.round(this.colors.length * this.d + (this.colors.length - 1) * gap) + 2, Math.round(this.d) + 2);
        }

        @Override
        protected void onDraw(Canvas c) {
            float gap = Ui.dp(getContext(), 2.0f);
            float r = this.d / 2.0f;
            float x = 1 + r;
            for (int col : this.colors) {
                this.paint.setStyle(Paint.Style.FILL);
                this.paint.setColor(col);
                c.drawCircle(x, 1 + r, r, this.paint);
                this.paint.setStyle(Paint.Style.STROKE);
                this.paint.setStrokeWidth(Math.max(1.0f, Ui.dp(getContext(), 0.6f)));
                this.paint.setColor(this.edge);
                c.drawCircle(x, 1 + r, r, this.paint);
                x += this.d + gap;
            }
        }
    }
}
