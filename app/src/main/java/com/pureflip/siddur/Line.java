package com.pureflip.siddur;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/**
 * A one-line label. A text that doesn't fit ends in "…" at rest. While its row is focused (selected) it
 * scrolls so the whole text can be read: it waits a moment showing the "…" version, glides to the end,
 * holds, and starts over. {@link #always()} keeps it scrolling whether focused or not (the Now card).
 * Text that fits never moves. Hebrew starts on the right and glides the other way.
 */
final class Line extends TextView {
    private boolean always;
    private boolean active;
    private float textW;
    private float avail;
    /** Created in the constructor; TextView's own constructor already calls setText (so may see it null). */
    private Marquee marquee;

    Line(Context c) {
        super(c);
        this.marquee = new Marquee(this);
        setSingleLine(true);
        setEllipsize(TextUtils.TruncateAt.END);
        setHorizontalFadingEdgeEnabled(false);
    }

    /** Scroll whenever the text doesn't fit, focused or not. */
    Line always() {
        this.always = true;
        setSelected(true);
        return this;
    }

    /** True while the label is allowed to scroll (focused / selected, or {@link #always()}). */
    boolean scrolls() {
        return this.active;
    }

    /** True while it is really scrolling (allowed to, and too long to fit). */
    boolean scrolling() {
        return this.marquee != null && this.marquee.running();
    }

    float scrollOffset() {
        return this.marquee == null ? 0.0f : this.marquee.offset();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected || this.always);
        boolean on = selected || this.always;
        if (on != this.active) {
            this.active = on;
            update();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        update();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        if (this.marquee == null) {
            return;
        }
        stop();
        post(new Runnable() {
            @Override
            public void run() {
                update();
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void update() {
        if (this.marquee == null) {
            return;
        }
        this.avail = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        this.textW = getPaint().measureText(getText().toString());
        boolean want = this.active && isAttachedToWindow() && this.avail > 0 && this.textW > this.avail + 0.5f;
        if (want) {
            this.marquee.start(this.textW - this.avail);
        } else {
            stop();
        }
    }

    private void stop() {
        if (this.marquee != null) {
            this.marquee.stop();
        }
    }

    private boolean rtlText() {
        Layout l = getLayout();
        if (l != null && l.getLineCount() > 0) {
            return l.getParagraphDirection(0) == Layout.DIR_RIGHT_TO_LEFT;
        }
        return getTextDirection() == View.TEXT_DIRECTION_RTL;
    }

    @Override
    protected void onDraw(Canvas c) {
        float offset = scrollOffset();
        if (!scrolling() || offset <= 0.0f) {
            super.onDraw(c); // at rest: the normal "…" text
            return;
        }
        int left = getCompoundPaddingLeft();
        int right = getWidth() - getCompoundPaddingRight();
        TextPaint tp = getPaint();
        tp.setColor(getCurrentTextColor());
        tp.drawableState = getDrawableState();
        android.graphics.Paint.Align was = tp.getTextAlign();
        tp.setTextAlign(android.graphics.Paint.Align.LEFT);
        c.save();
        c.clipRect(left + getScrollX(), 0, right + getScrollX(), getHeight());
        float x = rtlText() ? right - this.textW + offset : left - offset;
        String s = getText().toString();
        c.drawText(s, 0, s.length(), x + getScrollX(), getBaseline(), tp);
        c.restore();
        tp.setTextAlign(was);
    }
}
