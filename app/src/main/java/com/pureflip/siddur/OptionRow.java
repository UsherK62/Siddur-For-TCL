package com.pureflip.siddur;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * One popup option drawn as a single View (radio mark, label, optional colour dots, focus gradient + ring).
 * A city list is 33 of these instead of 33 x (LinearLayout + TextView + mark), which matters on a weak phone:
 * the popup opens with a fraction of the views to create, measure and lay out. Nothing is allocated in onDraw.
 */
final class OptionRow extends View {
    private final Pal p;
    private final boolean rtl;
    private final String text;
    private final int[] dots;
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float radius;
    private final float pad;
    private final float radioR;
    private final float dotD;
    private final float gap;
    private final android.graphics.Typeface faceRest;
    private final android.graphics.Typeface faceFocus;
    private LinearGradient focusShader;
    private LinearGradient markShader;
    private int shaderW = -1;
    /** The "…" version, drawn at rest and while the marquee waits at its start. */
    private CharSequence ell = "";
    private float glow;
    private boolean on;
    private ValueAnimator anim;
    /** Marquee: label width, room for it, current scroll offset and the looping animator. */
    private float textW;
    private float avail;
    private final Marquee marquee = new Marquee(this);

    OptionRow(Context c, Pal p, String lang, String text, int[] dots) {
        super(c);
        this.p = p;
        this.rtl = Str.rtl(lang);
        this.text = text;
        this.dots = dots;
        this.radius = Ui.dp(c, 9.0f);
        this.pad = Ui.dp(c, 9.0f);
        this.radioR = Ui.dp(c, 8.0f);
        this.dotD = Ui.dp(c, 8.0f);
        this.gap = Ui.dp(c, 8.0f);
        this.faceRest = Ui.face(lang, 600);
        this.faceFocus = Ui.face(lang, 700);
        this.tp.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13.5f, c.getResources().getDisplayMetrics()));
        this.tp.setColor(p.ink);
        this.tp.setTypeface(this.faceRest);
        this.ring.setStyle(Paint.Style.STROKE);
        this.ring.setStrokeWidth(Ui.dp(c, 1.5f));
        this.ring.setColor(p.ring);
        setFocusable(true);
        setClickable(true);
        setContentDescription(text);
    }

    CharSequence label() {
        return this.text;
    }

    boolean isChecked() {
        return this.on;
    }

    void setChecked(boolean v) {
        if (this.on != v) {
            this.on = v;
            invalidate();
        }
    }

    @Override
    protected void onFocusChanged(boolean gain, int direction, Rect previous) {
        super.onFocusChanged(gain, direction, previous);
        this.tp.setTypeface(gain ? this.faceFocus : this.faceRest);
        ellipsize(getWidth());
        updateMarquee();
        if (this.anim != null) {
            this.anim.cancel();
        }
        this.anim = ValueAnimator.ofFloat(this.glow, gain ? 1.0f : 0.0f);
        this.anim.setDuration(SetRow.EXPAND_MS);
        this.anim.setInterpolator(new DecelerateInterpolator(1.6f));
        this.anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                glow = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        this.anim.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), MeasureSpec.getSize(hSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        ellipsize(w);
        updateMarquee();
    }

    /** True while the label is scrolling (focused and too long to fit). */
    boolean scrolling() {
        return this.marquee.running();
    }

    float scrollOffset() {
        return this.marquee.offset();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.marquee.stop();
        super.onDetachedFromWindow();
    }



    /** A label that doesn't fit scrolls while focused (see {@link Marquee}). */
    private void updateMarquee() {
        boolean want = isFocused() && isAttachedToWindow() && this.avail > 0 && this.textW > this.avail + 0.5f;
        if (want) {
            this.marquee.start(this.textW - this.avail);
        } else {
            this.marquee.stop();
        }
    }

    private float dotsWidth() {
        return this.dots == null ? 0 : this.dots.length * this.dotD + (this.dots.length - 1) * Ui.dp(getContext(), 2.0f);
    }

    private void ellipsize(int w) {
        if (w <= 0) {
            return;
        }
        float avail = w - 2 * this.pad - 2 * this.radioR - this.gap - (this.dots == null ? 0 : dotsWidth() + this.gap);
        this.avail = Math.max(0, avail);
        this.textW = this.tp.measureText(this.text);
        this.ell = TextUtils.ellipsize(this.text, this.tp, this.avail, TextUtils.TruncateAt.END);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float cy = h / 2.0f;
        if (this.shaderW != getWidth()) {
            this.focusShader = new LinearGradient(0, 0, w, 0, this.p.focus0, this.p.focus1, Shader.TileMode.CLAMP);
            this.markShader = new LinearGradient(0, 0, 2 * this.radioR, 0, this.p.acc, this.p.acc2, Shader.TileMode.CLAMP);
            this.fill.setShader(this.focusShader);
            this.shaderW = getWidth();
        }
        if (this.glow > 0.0f) {
            int a = Math.round(255 * this.glow);
            this.rect.set(0, 0, w, h);
            this.fill.setAlpha(a);
            c.drawRoundRect(this.rect, this.radius, this.radius, this.fill);
            float half = this.ring.getStrokeWidth() / 2.0f;
            this.rect.inset(half, half);
            this.ring.setAlpha(a);
            c.drawRoundRect(this.rect, this.radius - half, this.radius - half, this.ring);
        }
        // radio mark on the leading side
        float rx = this.rtl ? w - this.pad - this.radioR : this.pad + this.radioR;
        if (this.on) {
            c.save();
            c.translate(rx - this.radioR, 0);
            this.mark.setStyle(Paint.Style.FILL);
            this.mark.setColor(0xFF000000); // opaque: the swatch edges below leave this paint at 25 % alpha
            this.mark.setShader(this.markShader);
            c.drawCircle(this.radioR, cy, this.radioR, this.mark);
            c.restore();
            this.mark.setShader(null);
            this.mark.setColor(this.p.dark ? this.p.pillFg : 0xFFFFFFFF);
            c.drawCircle(rx, cy, this.radioR * 0.38f, this.mark);
        } else {
            float sw = Ui.dp(getContext(), 1.6f);
            this.mark.setShader(null);
            this.mark.setStyle(Paint.Style.STROKE);
            this.mark.setStrokeWidth(sw);
            this.mark.setColor(this.p.off);
            c.drawCircle(rx, cy, this.radioR - sw / 2.0f, this.mark);
        }
        // label next to the mark
        float base = cy - (this.tp.descent() + this.tp.ascent()) / 2.0f;
        float x0 = this.rtl ? rx - this.radioR - this.gap : rx + this.radioR + this.gap;
        float scrollX = this.marquee.offset();
        boolean clip = this.marquee.running() && scrollX > 0.0f;
        CharSequence draw = clip ? this.text : this.ell;
        if (clip) {
            c.save();
            if (this.rtl) {
                c.clipRect(x0 - this.avail, 0, x0, h);
            } else {
                c.clipRect(x0, 0, x0 + this.avail, h);
            }
        }
        if (this.rtl) {
            this.tp.setTextAlign(Paint.Align.RIGHT);
            c.drawText(draw, 0, draw.length(), x0 + scrollX, base, this.tp);
        } else {
            this.tp.setTextAlign(Paint.Align.LEFT);
            c.drawText(draw, 0, draw.length(), x0 - scrollX, base, this.tp);
        }
        if (clip) {
            c.restore();
        }
        // colour dots on the far side
        if (this.dots != null) {
            float d = this.dotD;
            float step = d + Ui.dp(getContext(), 2.0f);
            float x = this.rtl ? this.pad + d / 2.0f : w - this.pad - dotsWidth() + d / 2.0f;
            for (int col : this.dots) {
                this.mark.setStyle(Paint.Style.FILL);
                this.mark.setColor(col);
                c.drawCircle(x, cy, d / 2.0f, this.mark);
                this.mark.setStyle(Paint.Style.STROKE);
                this.mark.setStrokeWidth(Math.max(1.0f, Ui.dp(getContext(), 0.6f)));
                this.mark.setColor(this.p.dark ? 0x66FFFFFF : 0x40000000);
                c.drawCircle(x, cy, d / 2.0f, this.mark);
                x += step;
            }
        }
    }
}
