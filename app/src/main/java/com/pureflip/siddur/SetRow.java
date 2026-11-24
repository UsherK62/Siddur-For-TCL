package com.pureflip.siddur;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One Settings row, Auto Reply style. Label on the leading side (right in Hebrew), value / switch / chevron
 * on the trailing side. When focused the row grows from {@code rest} to {@code open} dp in 120 ms and
 * draws a soft focus gradient with a 1.5 dp accent ring (radius 9 dp).
 */
final class SetRow extends LinearLayout {
    static final long EXPAND_MS = 120L;
    final TextView label;
    final String key;
    private final Pal p;
    private final String lang;
    private final int restPx;
    private final int openPx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float radius;
    private final float labelRest;
    private final float labelOpen;
    private float glow;
    /** Focus gradient, rebuilt only when the width changes (the row's height animates, its width doesn't). */
    private LinearGradient shader;
    private int shaderW = -1;
    private ValueAnimator anim;
    private boolean open;
    private View value;
    private TextView tint;

    SetRow(Context c, Pal p, String lang, String key, String text, float restDp, float openDp, float labelSp) {
        super(c);
        this.p = p;
        this.lang = lang;
        this.key = key;
        this.restPx = Ui.dp(c, restDp);
        this.openPx = Ui.dp(c, openDp);
        this.radius = Ui.dp(c, 9.0f);
        this.labelRest = labelSp;
        this.labelOpen = openDp > restDp ? labelSp + 1.0f : labelSp;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int pad = Ui.dp(c, 9.0f);
        setPadding(pad, 0, pad, 0);
        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);
        this.ring.setStyle(Paint.Style.STROKE);
        this.ring.setStrokeWidth(Ui.dp(c, 1.5f));
        this.ring.setColor(p.ring);
        this.label = Ui.line(c, text, labelSp, p.ink, Ui.face(lang, 600));
        if (Str.rtl(lang)) {
            Ui.hebrew(this.label);
        } else {
            Ui.latin(this.label);
        }
        layoutChildren(null);
    }

    boolean rtl() {
        return Str.rtl(this.lang);
    }

    int restPx() {
        return this.restPx;
    }

    int openPx() {
        return this.openPx;
    }

    boolean isOpen() {
        return this.open;
    }

    /** Puts {@code v} on the trailing side (left in RTL). */
    void setValue(View v) {
        setValue(v, v instanceof TextView ? (TextView) v : null);
    }

    /** As {@link #setValue(View)}; {@code tint} is the value text that turns accent while focused. */
    void setValue(View v, TextView tint) {
        this.value = v;
        this.tint = tint;
        if (tint != null) {
            tint.setTextColor(this.open ? this.p.acc : this.p.ink2);
        }
        layoutChildren(v);
    }

    View value() {
        return this.value;
    }

    private void layoutChildren(View v) {
        removeAllViews();
        LinearLayout.LayoutParams grow = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        LinearLayout.LayoutParams wrap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (rtl()) {
            if (v != null) {
                wrap.rightMargin = Ui.dp(getContext(), 8.0f);
                addView(v, wrap);
            }
            addView(this.label, grow);
        } else {
            addView(this.label, grow);
            if (v != null) {
                wrap.leftMargin = Ui.dp(getContext(), 8.0f);
                addView(v, wrap);
            }
        }
    }

    /** Opens (focus) or closes the row; animated over 120 ms unless {@code animate} is false. */
    void setOpen(boolean o, boolean animate) {
        this.open = o;
        this.label.setTypeface(Ui.face(this.lang, o ? 700 : 600));
        this.label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, o ? this.labelOpen : this.labelRest);
        if (this.tint != null) {
            this.tint.setTextColor(o ? this.p.acc : this.p.ink2);
        }
        if (this.anim != null) {
            this.anim.cancel();
        }
        final ViewGroup.LayoutParams lp = getLayoutParams();
        final int toH = o ? this.openPx : this.restPx;
        final float toG = o ? 1.0f : 0.0f;
        if (!animate || lp == null) {
            if (lp != null) {
                lp.height = toH;
                setLayoutParams(lp);
            }
            this.glow = toG;
            invalidate();
            return;
        }
        final int fromH = lp.height > 0 ? lp.height : getHeight();
        final float fromG = this.glow;
        this.anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim.setDuration(EXPAND_MS);
        this.anim.setInterpolator(new DecelerateInterpolator(1.6f));
        this.anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                int h = Math.round(fromH + (toH - fromH) * t);
                glow = fromG + (toG - fromG) * t;
                if (h != lp.height) {
                    // only re-layout on frames where the pixel height really changes (at mdpi ~9 of them)
                    lp.height = h;
                    setLayoutParams(lp);
                }
                invalidate();
            }
        });
        this.anim.start();
    }

    /** Height this row ends at once its animation is done. */
    int endHeight() {
        return this.open ? this.openPx : this.restPx;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (this.glow > 0.0f) {
            float w = getWidth();
            float h = getHeight();
            this.rect.set(0, 0, w, h);
            if (this.shader == null || this.shaderW != getWidth()) {
                this.shader = new LinearGradient(0, 0, w, 0, this.p.focus0, this.p.focus1, Shader.TileMode.CLAMP);
                this.shaderW = getWidth();
                this.fill.setShader(this.shader);
            }
            this.fill.setAlpha(Math.round(255 * this.glow));
            c.drawRoundRect(this.rect, this.radius, this.radius, this.fill);
            float half = this.ring.getStrokeWidth() / 2.0f;
            this.rect.inset(half, half);
            this.ring.setAlpha(Math.round(255 * this.glow));
            c.drawRoundRect(this.rect, this.radius - half, this.radius - half, this.ring);
        }
        super.onDraw(c);
    }
}
