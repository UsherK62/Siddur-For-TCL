package com.pureflip.siddur;

import android.os.SystemClock;
import android.view.View;

/**
 * Scroll offset for a one-line label that doesn't fit: wait {@link #DELAY_MS}, glide to the end at
 * {@link #DP_PER_S}, hold {@link #HOLD_MS}, jump back, repeat. Driven by animation frames only while it
 * glides (posted delays during the waits), and independent of the system animator scale, so it still
 * scrolls with animations turned off (like TextView's own marquee).
 */
final class Marquee implements Runnable {
    static final long DELAY_MS = 900L;
    static final long HOLD_MS = 1100L;
    static final float DP_PER_S = 30.0f;

    private final View view;
    private float dist;
    private long glide;
    private long start;
    private boolean running;
    private float offset;

    Marquee(View view) {
        this.view = view;
    }

    boolean running() {
        return this.running;
    }

    float offset() {
        return this.offset;
    }

    /** Starts (or keeps running) a loop over {@code distPx}. */
    void start(float distPx) {
        if (this.running && Math.abs(distPx - this.dist) < 0.5f) {
            return;
        }
        stop();
        this.dist = distPx;
        float density = this.view.getResources().getDisplayMetrics().density;
        this.glide = Math.max(400L, Math.round(distPx / (DP_PER_S * density) * 1000.0f));
        this.start = SystemClock.uptimeMillis();
        this.running = true;
        this.view.postDelayed(this, DELAY_MS);
    }

    void stop() {
        this.view.removeCallbacks(this);
        this.running = false;
        if (this.offset != 0.0f) {
            this.offset = 0.0f;
            this.view.invalidate();
        }
    }

    @Override
    public void run() {
        if (!this.running) {
            return;
        }
        long total = DELAY_MS + this.glide + HOLD_MS;
        long t = (SystemClock.uptimeMillis() - this.start) % total;
        float x;
        long next; // ms until the next change; 0 = next frame
        if (t < DELAY_MS) {
            x = 0.0f;
            next = DELAY_MS - t;
        } else if (t < DELAY_MS + this.glide) {
            x = this.dist * (t - DELAY_MS) / this.glide;
            next = 0;
        } else {
            x = this.dist;
            next = total - t;
        }
        if (x != this.offset) {
            this.offset = x;
            this.view.invalidate();
        }
        if (next == 0) {
            this.view.postOnAnimation(this);
        } else {
            this.view.postDelayed(this, Math.max(1L, next));
        }
    }
}
