package com.pureflip.siddur;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReaderActivity extends BaseActivity {
    private Set<String> active;
    private Adapter adapter;
    private int cur;
    private Doc d;
    private String doc;
    private int from;
    private ValueAnimator glideAnim;
    private ListView list;
    private TextView loadingView;
    private String nusach;
    private View progFill;
    private TextView progText;
    private float textSp;
    private int to;
    private final List<Integer> vis = new ArrayList<>();
    private final Set<Integer> expanded = new HashSet<>();
    private int pendingPara = -1;



    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String stringExtra = getIntent().getStringExtra("nusach");
        this.nusach = stringExtra;
        if (stringExtra == null) {
            this.nusach = this.prefs.nusach();
        }
        String stringExtra2 = getIntent().getStringExtra("doc");
        this.doc = stringExtra2;
        if (stringExtra2 == null) {
            this.doc = "shacharis";
        }
        this.from = getIntent().getIntExtra("from", 0);
        this.to = getIntent().getIntExtra("to", -1);
        this.pendingPara = bundle != null ? bundle.getInt("para", -1) : getIntent().getIntExtra("para", -1);
        String stringExtra3 = getIntent().getStringExtra("he");
        if (stringExtra3 == null) {
            stringExtra3 = Names.docHe(this.doc);
        }
        String stringExtra4 = getIntent().getStringExtra("en");
        if (stringExtra4 == null) {
            stringExtra4 = Names.docEn(this.doc);
        }
        if (Str.rtl(this.lang)) {
            stringExtra4 = null;
        }
        setHeader(stringExtra4, stringExtra3);
        this.headRight.setTypeface(Ui.frankBold);
        this.headRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f);
        this.textSp = this.prefs.textSp();
        Luach current = Luach.current(this.prefs);
        this.active = current.active(this.doc);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int i = "shacharis".equals(this.doc) ? 0 : "mincha".equals(this.doc) ? 1 : "maariv".equals(this.doc) ? 2 : -1;
        if (i >= 0) {
            TextView text = Ui.text(this, current.insertions(i), 11.5f, this.pal.acc, Ui.plexSemi);
            Ui.hebrew(text);
            text.setMaxLines(2);
            text.setPadding(Ui.dp(this, 10.0f), Ui.dp(this, 3.0f), Ui.dp(this, 10.0f), 0);
            linearLayout.addView(text, new LinearLayout.LayoutParams(-1, -2));
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout2.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout2.setPadding(Ui.dp(this, 10.0f), Ui.dp(this, 3.0f), Ui.dp(this, 10.0f), Ui.dp(this, 2.0f));
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackground(Ui.round(this, this.pal.dark ? this.pal.line : Pal.mix(this.pal.line, this.pal.bg, 0.35f), 2.0f, 0, 0.0f));
        View view = new View(this);
        this.progFill = view;
        view.setBackground(Ui.round(this, this.pal.acc, 2.0f, 0, 0.0f));
        this.progFill.setScaleX(0.0f);
        frameLayout.addView(this.progFill, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.setClipChildren(true);
        linearLayout2.addView(frameLayout, new LinearLayout.LayoutParams(0, Ui.dp(this, 3.0f), 1.0f));
        TextView line = Ui.line(this, "", 10.5f, this.pal.ink2, Ui.plexSemi);
        this.progText = line;
        line.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = Ui.dp(this, 7.0f);
        linearLayout2.addView(this.progText, layoutParams);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(-1, -2));
        ListView listView = new ListView(this);
        this.list = listView;
        listView.setDivider(null);
        this.list.setSelector(new ColorDrawable(0));
        this.list.setCacheColorHint(0);
        this.list.setVerticalScrollBarEnabled(false);
        this.list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        this.list.setFocusable(false);
        this.list.setItemsCanFocus(false);
        this.list.setClipToPadding(false);
        this.list.setPadding(0, Ui.dp(this, 2.0f), 0, Ui.dp(this, 40.0f));
        this.list.setAlpha(0.0f);
        this.list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView absListView, int i2, int i3, int i4) {
            }

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i2) {
            }
        });
        FrameLayout frameLayout2 = new FrameLayout(this);
        frameLayout2.addView(this.list, new FrameLayout.LayoutParams(-1, -1));
        final TextView loading = Ui.line(this, "• • •", 14.0f, this.pal.ink2, Ui.plexBold);
        loading.setGravity(Gravity.CENTER);
        loading.setAlpha(0.0f);
        frameLayout2.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        // Only show the dots if loading takes a noticeable moment.
        loading.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (d == null) {
                    loading.animate().alpha(1.0f).setDuration(200L).start();
                }
            }
        }, 300L);
        this.loadingView = loading;
        linearLayout.addView(frameLayout2, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.body.addView(linearLayout, fill());
        Doc.load(this, this.nusach, this.doc, new Doc.Callback() {
            @Override
            public void done(Doc loaded) {
                if (isFinishing()) {
                    return;
                }
                if (loaded == null || loaded.items.isEmpty()) {
                    finish(); // text missing: go back rather than show the loading dots forever
                    return;
                }
                d = loaded;
                if (to < 0 || to > d.items.size()) {
                    to = d.items.size();
                }
                tehillimRange();
                rebuildVisible();
                adapter = new Adapter();
                list.setAdapter(adapter);
                // Restore the saved / bookmarked paragraph when it is still visible.
                int start = 0;
                if (pendingPara >= 0) {
                    int at = visIndexOf(pendingPara);
                    if (at >= 0) {
                        start = at;
                    }
                }
                cur = start;
                list.setSelectionFromTop(cur, Ui.dp(ReaderActivity.this, 4.0f));
                list.animate().alpha(1.0f).setDuration(220L).start();
                if (loadingView != null) {
                    loadingView.animate().cancel();
                    loadingView.setVisibility(View.GONE);
                }
                updateProgress(false);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.d == null || this.cur >= this.vis.size()) {
            return;
        }
        bundle.putInt("para", this.vis.get(this.cur).intValue());
    }

    private void tehillimRange() {
        int i = 0;
        int intExtra = getIntent().getIntExtra("chFrom", 0);
        int intExtra2 = getIntent().getIntExtra("chTo", 0);
        if (!"tehillim".equals(this.doc) || intExtra <= 0) {
            return;
        }
        int intExtra3 = getIntent().getIntExtra("vFrom", 0);
        int intExtra4 = getIntent().getIntExtra("vTo", 0);
        int size = this.d.items.size();
        int i2 = 0;
        int i3 = 0;
        while (true) {
            if (i >= this.d.items.size()) {
                break;
            }
            if (this.d.items.get(i).heading) {
                i2++;
                if (i2 == intExtra) {
                    i3 = i;
                }
                if (i2 == intExtra2 + 1) {
                    size = i;
                    break;
                }
            }
            i++;
        }
        if (intExtra3 > 1) {
            i3 += intExtra3;
        }
        if (intExtra4 > 0) {
            size = Math.min(size, (intExtra3 > 1 ? i3 - intExtra3 : i3) + intExtra4 + 1);
        }
        this.from = i3;
        this.to = size;
    }

    /** secUpTo[k] = level-1 headings among items from..from+k; total in secTotal. Built once per range. */
    private int[] secUpTo;
    private int secTotal;

    private void buildSections() {
        int n = Math.max(0, this.to - this.from);
        this.secUpTo = new int[n];
        int c = 0;
        for (int k = 0; k < n; k++) {
            Doc.Item item = this.d.items.get(this.from + k);
            if (item.heading && item.level == 1) {
                c++;
            }
            this.secUpTo[k] = c;
        }
        this.secTotal = c;
    }

    private void rebuildVisible() {
        if (this.secUpTo == null || this.secUpTo.length != Math.max(0, this.to - this.from)) {
            buildSections();
        }
        this.vis.clear();
        int i = -1;
        for (int i2 = this.from; i2 < this.to; i2++) {
            Doc.Item item = this.d.items.get(i2);
            if (i >= 0) {
                if (item.heading && item.level <= i) {
                    i = -1;
                }
            }
            this.vis.add(Integer.valueOf(i2));
            if (item.heading && !item.activeIn(this.active) && !this.expanded.contains(Integer.valueOf(i2))) {
                i = item.level;
            }
        }
    }

    private int visIndexOf(int i) {
        for (int i2 = 0; i2 < this.vis.size(); i2++) {
            if (this.vis.get(i2).intValue() >= i) {
                return i2;
            }
        }
        return this.vis.size() - 1;
    }

    private boolean collapsed(int i) {
        Doc.Item item = this.d.items.get(i);
        return (!item.heading || item.activeIn(this.active) || this.expanded.contains(Integer.valueOf(i))) ? false : true;
    }

    final class ParaView extends TextView {
        ValueAnimator anim;
        float mark;
        private final Paint p;
        private final RectF r;
        private final float barW;
        private final float barEnd;
        private final float barInset;

        ParaView(Context context) {
            super(context);
            this.barW = Ui.dp(context, 2.5f);
            this.barEnd = Ui.dp(context, 5.0f);
            this.barInset = Ui.dp(context, 4.0f);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.p = paint;
            this.r = new RectF();
            paint.setColor(ReaderActivity.this.pal.acc);
            setTextDirection(4);
            setGravity(Gravity.RIGHT);
            setIncludeFontPadding(true);
        }

        void setMark(boolean z, boolean z2) {
            float f = z ? 1.0f : 0.0f;
            ValueAnimator valueAnimator = this.anim;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            if (!z2) {
                this.mark = f;
                invalidate();
                return;
            }
            ValueAnimator ofFloat = ValueAnimator.ofFloat(this.mark, f);
            this.anim = ofFloat;
            ofFloat.setDuration(160L);
            this.anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    ParaView.this.mark = ((Float) valueAnimator2.getAnimatedValue()).floatValue();
                    ParaView.this.invalidate();
                }
            });
            this.anim.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (this.mark > 0.01f) {
                float dp = this.barW;
                float width = getWidth() - this.barEnd;
                this.r.set(width - dp, this.barInset, width, getHeight() - this.barInset);
                this.p.setAlpha(Math.round(this.mark * 255.0f * (ReaderActivity.this.pal.dark ? 0.85f : 0.7f)));
                canvas.drawRoundRect(this.r, dp, dp, this.p);
            }
        }
    }

    final class Adapter extends BaseAdapter {
        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int i) {
            return false;
        }

        Adapter() {
        }

        @Override
        public int getCount() {
            return ReaderActivity.this.vis.size();
        }

        @Override
        public Object getItem(int i) {
            return ReaderActivity.this.d.items.get(((Integer) ReaderActivity.this.vis.get(i)).intValue());
        }

        @Override
        public int getItemViewType(int i) {
            return ReaderActivity.this.d.items.get(((Integer) ReaderActivity.this.vis.get(i)).intValue()).heading ? 1 : 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ParaView paraView;
            int alpha;
            if (view instanceof ParaView) {
                paraView = (ParaView) view;
            } else {
                ReaderActivity readerActivity = ReaderActivity.this;
                paraView = readerActivity.new ParaView(readerActivity);
            }
            int intValue = ((Integer) ReaderActivity.this.vis.get(i)).intValue();
            Doc.Item item = ReaderActivity.this.d.items.get(intValue);
            boolean activeIn = item.activeIn(ReaderActivity.this.active);
            if (item.heading) {
                paraView.setTypeface(Ui.frankBold);
                paraView.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.level == 1 ? ReaderActivity.this.textSp * 1.0f : ReaderActivity.this.textSp * 0.92f);
                paraView.setTextColor(item.level == 1 ? ReaderActivity.this.pal.acc : ReaderActivity.this.pal.ink);
                paraView.setLineSpacing(0.0f, 1.0f);
                paraView.setPadding(Ui.dp(ReaderActivity.this, 10.0f), Ui.dp(ReaderActivity.this, item.level != 1 ? 7.0f : 10.0f), Ui.dp(ReaderActivity.this, 13.0f), Ui.dp(ReaderActivity.this, 2.0f));
                if (ReaderActivity.this.collapsed(intValue)) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(item.plain);
                    spannableStringBuilder.append('\n');
                    int length = spannableStringBuilder.length();
                    spannableStringBuilder.append((CharSequence) Str.get(ReaderActivity.this.lang, "notSaid"));
                    spannableStringBuilder.setSpan(new RelativeSizeSpan(0.62f), length, spannableStringBuilder.length(), 33);
                    spannableStringBuilder.setSpan(new ForegroundColorSpan(ReaderActivity.this.pal.ink2), length, spannableStringBuilder.length(), 33);
                    spannableStringBuilder.setSpan(new Doc.FaceSpan(Ui.plexSemi), length, spannableStringBuilder.length(), 33);
                    paraView.setText(spannableStringBuilder);
                    paraView.setTextColor(Pal.alpha(item.level == 1 ? ReaderActivity.this.pal.acc : ReaderActivity.this.pal.ink, 0.6f));
                } else {
                    paraView.setText(item.plain);
                }
            } else {
                paraView.setTypeface(Ui.frank);
                float f = ReaderActivity.this.textSp;
                if (!activeIn) {
                    f *= 0.86f;
                }
                paraView.setTextSize(TypedValue.COMPLEX_UNIT_SP, f);
                Pal pal = ReaderActivity.this.pal;
                if (activeIn) {
                    alpha = pal.ink;
                } else {
                    alpha = Pal.alpha(pal.ink2, ReaderActivity.this.pal.dark ? 0.55f : 0.5f);
                }
                paraView.setTextColor(alpha);
                paraView.setLineSpacing(0.0f, 1.12f);
                paraView.setPadding(Ui.dp(ReaderActivity.this, 10.0f), Ui.dp(ReaderActivity.this, 3.0f), Ui.dp(ReaderActivity.this, 13.0f), Ui.dp(ReaderActivity.this, 4.0f));
                paraView.setText(item.text);
            }
            paraView.setMark(i == ReaderActivity.this.cur, false);
            return paraView;
        }
    }

    private ParaView viewAt(int i) {
        View childAt = this.list.getChildAt(i - this.list.getFirstVisiblePosition());
        if (childAt instanceof ParaView) {
            return (ParaView) childAt;
        }
        return null;
    }

    private void setCur(int i, boolean z) {
        int i2;
        if (i < 0 || i >= this.vis.size() || i == (i2 = this.cur)) {
            return;
        }
        ParaView viewAt = viewAt(i2);
        if (viewAt != null) {
            viewAt.setMark(false, z);
        }
        this.cur = i;
        ParaView viewAt2 = viewAt(i);
        if (viewAt2 != null) {
            viewAt2.setMark(true, z);
        }
        updateProgress(true);
    }

    /** Scroll still owed by the running glide (0 when idle). */
    private int glideTotal;
    private int glideDone;
    private float glideCarry;
    private android.animation.TimeAnimator glider;
    /** Glide time constant: ~95 % of the distance in 3 tau = 165 ms, close to the old 170 ms decelerate. */
    private static final float GLIDE_TAU_MS = 55.0f;

    private int pendingGlide() {
        return this.glider != null ? this.glideTotal - this.glideDone : 0;
    }

    /** Stops the glide where it is (no jump to its end). */
    private void stopGlide() {
        if (this.glider != null) {
            android.animation.TimeAnimator g = this.glider;
            this.glider = null;
            g.cancel();
        }
        this.glideTotal = 0;
        this.glideDone = 0;
        this.glideCarry = 0.0f;
    }

    /** Finishes the running glide at once (for jumps that rebuild the list anyway). */
    private void finishGlide() {
        int left = pendingGlide();
        stopGlide();
        if (left != 0) {
            this.list.scrollListBy(left);
        }
    }

    /**
     * Glides the list by {@code by} px. One frame-driven animator chases the target (exponential ease-out),
     * and a new glide while one is running just moves the target. A held D-pad (a repeat every ~50 ms) so
     * gives one continuous movement, instead of restarting an animation each repeat (a still frame) or
     * jumping the old one to its end (a snap) as before.
     */
    private void glide(int by, int ms) {
        if (by == 0) {
            return;
        }
        if (this.glider != null) {
            this.glideTotal += by;
            return;
        }
        this.glideTotal = by;
        this.glideDone = 0;
        this.glideCarry = 0.0f;
        final android.animation.TimeAnimator g = new android.animation.TimeAnimator();
        this.glider = g;
        g.setTimeListener(new android.animation.TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(android.animation.TimeAnimator a, long total, long dt) {
                if (a != glider) {
                    return;
                }
                if (dt <= 0) {
                    dt = 16; // first callback: move on this frame already, no still frame
                }
                int left = glideTotal - glideDone;
                float k = 1.0f - (float) Math.exp(-dt / GLIDE_TAU_MS);
                float want = left * k + glideCarry;
                int d = (int) want;
                glideCarry = want - d;
                if (d == 0 && left != 0 && Math.abs(left) <= 2) {
                    d = left; // last pixel or two
                }
                if (d != 0) {
                    glideDone += d;
                    list.scrollListBy(d);
                }
                if (glideTotal == glideDone) {
                    stopGlide();
                }
            }
        });
        g.start();
    }

    private static final DecelerateInterpolator PROGRESS_EASE = new DecelerateInterpolator();

    private int margin() {
        return Ui.dp(this, 6.0f);
    }

    private void step(int i) {
        int i2;
        if (this.d == null || this.vis.isEmpty()) {
            return;
        }
        // Positions below are where things will be once the running glide is done.
        int owed = pendingGlide();
        int height = (this.list.getHeight() - this.list.getPaddingBottom()) + Ui.dp(this, 20.0f);
        int margin = margin();
        int height2 = (int) (this.list.getHeight() * 0.72f);
        ParaView viewAt = viewAt(this.cur);
        if (i > 0) {
            if (viewAt != null && viewAt.getBottom() - owed > (i2 = height - margin)) {
                glide(Math.min(viewAt.getBottom() - owed - i2, height2), 170);
                return;
            } else {
                if (this.cur < this.vis.size() - 1) {
                    setCur(this.cur + 1, true);
                    ensureVisible(this.cur, 1);
                    return;
                }
                return;
            }
        }
        if (viewAt != null && viewAt.getTop() - owed < margin) {
            glide(-Math.min(margin - (viewAt.getTop() - owed), height2), 170);
            return;
        }
        int i3 = this.cur;
        if (i3 > 0) {
            setCur(i3 - 1, true);
            ensureVisible(this.cur, -1);
        }
    }

    private void ensureVisible(int i, int i2) {
        int i3;
        int height = (this.list.getHeight() - this.list.getPaddingBottom()) + Ui.dp(this, 20.0f);
        int margin = margin();
        ParaView viewAt = viewAt(i);
        int owed = pendingGlide();
        if (viewAt == null) {
            // The paragraph isn't laid out yet (just below / above the screen). Glide its neighbour's edge
            // into place instead of jumping the whole list (that jump was a full-screen snap on key repeat).
            int cap = (int) (this.list.getHeight() * 0.72f);
            ParaView nb = viewAt(i - (i2 > 0 ? 1 : -1));
            if (nb != null && i2 > 0) {
                // show the first quarter screen of it; the next key (it's laid out by then) does the rest
                glide(Math.max(0, Math.min(cap, nb.getBottom() - owed - (height - margin) + this.list.getHeight() / 4)), 170);
                return;
            }
            if (nb != null && i2 < 0) {
                glide(Math.min(0, Math.max(-cap, nb.getTop() - owed - margin - this.list.getHeight() / 4)), 170);
                return;
            }
            stopGlide();
            this.list.setSelectionFromTop(i, margin);
            return;
        }
        int top = viewAt.getTop() - owed;
        int bottom = viewAt.getBottom() - owed;
        if (i2 > 0 && bottom > (i3 = height - margin)) {
            glide(Math.max(0, Math.min(bottom - i3, top - margin)), 170);
        } else {
            if (i2 >= 0 || top >= margin) {
                return;
            }
            glide(Math.min(0, Math.max(top - margin, bottom - (height - margin))), 170);
        }
    }

    private void page(int i) {
        if (this.d == null) {
            return;
        }
        glide(((int) (this.list.getHeight() * 0.8f)) * i, 220);
        this.list.postDelayed(new Runnable() {
            @Override
            public void run() {
                int firstVisiblePosition = ReaderActivity.this.list.getFirstVisiblePosition();
                for (int i2 = 0; i2 < ReaderActivity.this.list.getChildCount(); i2++) {
                    if (ReaderActivity.this.list.getChildAt(i2).getTop() >= 0 || i2 == ReaderActivity.this.list.getChildCount() - 1) {
                        ReaderActivity.this.setCur(firstVisiblePosition + i2, true);
                        return;
                    }
                }
            }
        }, 250L);
    }

    private void jumpTo(final int target) {
        finishGlide();
        if (target < 0 || target >= this.vis.size()) {
            return;
        }
        ParaView viewAt = viewAt(this.cur);
        if (viewAt != null) {
            viewAt.setMark(false, false);
        }
        this.cur = target;
        ParaView viewAt2 = viewAt(target);
        if (viewAt2 != null) {
            int top = viewAt2.getTop() - margin();
            if (top != 0) {
                glide(top, 220);
            }
            this.list.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ReaderActivity readerActivity = ReaderActivity.this;
                    ParaView viewAt3 = readerActivity.viewAt(readerActivity.cur);
                    if (viewAt3 != null) {
                        viewAt3.setMark(true, true);
                    }
                }
            }, 240L);
        } else {
            this.list.animate().cancel();
            this.list.animate().alpha(0.0f).setDuration(90L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    ReaderActivity.this.list.setSelectionFromTop(target, ReaderActivity.this.margin());
                    ReaderActivity.this.adapter.notifyDataSetChanged();
                    ReaderActivity.this.list.animate().alpha(1.0f).setDuration(160L).start();
                }
            }).start();
        }
        updateProgress(true);
    }

    /** Jumps to the next / previous section heading (level 1 or 2); past the ends goes to the last / first paragraph. */
    private void section(int dir) {
        if (this.d == null) {
            return;
        }
        for (int i = this.cur + dir; i >= 0 && i < this.vis.size(); i += dir) {
            Doc.Item item = this.d.items.get(this.vis.get(i));
            if (item.heading && item.level <= 2) {
                jumpTo(i);
                return;
            }
        }
        jumpTo(dir > 0 ? this.vis.size() - 1 : 0);
    }

    private void updateProgress(boolean z) {
        if (this.d == null || this.vis.isEmpty()) {
            return;
        }
        float size = this.vis.size() <= 1 ? 1.0f : (float) this.cur / (this.vis.size() - 1);
        View view = this.progFill;
        view.setPivotX(view.getWidth() > 0 ? this.progFill.getWidth() : 10000.0f);
        if (this.progFill.getWidth() == 0) {
            this.progFill.post(new Runnable() {
                @Override
                public void run() {
                    ReaderActivity.this.progFill.setPivotX(ReaderActivity.this.progFill.getWidth());
                }
            });
        }
        this.progFill.animate().cancel();
        if (z) {
            this.progFill.animate().scaleX(Math.max(0.015f, size)).setDuration(220L).setInterpolator(PROGRESS_EASE).start();
        } else {
            this.progFill.setScaleX(Math.max(0.015f, size));
        }
        int intValue = this.vis.get(Math.max(0, Math.min(this.cur, this.vis.size() - 1))).intValue();
        int k = intValue - this.from;
        int i = this.secTotal;
        int i2 = (this.secUpTo != null && k >= 0 && k < this.secUpTo.length) ? this.secUpTo[k] : 0;
        if (i == 0) {
            i = 1;
            i2 = 1;
        }
        this.progText.setText(Math.max(1, i2) + " / " + i);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                step(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                step(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: // the text runs right to left, so left is "forward"
                section(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                section(-1);
                return true;
            case KeyEvent.KEYCODE_8:
                page(1);
                return true;
            case KeyEvent.KEYCODE_2:
                page(-1);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return super.onKeyDown(keyCode, event);
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected boolean onOkKey() {
        if (this.d == null) {
            return true;
        }
        if (this.vis.isEmpty() || this.cur < 0 || this.cur >= this.vis.size()) {
            return true;
        }
        int intValue = this.vis.get(this.cur).intValue();
        if (collapsed(intValue)) {
            finishGlide();
            int firstVisiblePosition = this.list.getFirstVisiblePosition();
            View childAt = this.list.getChildAt(0);
            int intValue2 = firstVisiblePosition < this.vis.size() ? this.vis.get(firstVisiblePosition).intValue() : intValue;
            int top = childAt != null ? childAt.getTop() : margin();
            this.expanded.add(Integer.valueOf(intValue));
            rebuildVisible();
            this.cur = visIndexOf(intValue);
            this.adapter.notifyDataSetChanged();
            this.list.setSelectionFromTop(visIndexOf(intValue2), top);
            step(1);
            return true;
        }
        page(1);
        return true;
    }

    @Override
    protected void onMenuKey() {
        if (this.d == null) {
            return;
        }
        Popup newPopup = newPopup(null);
        newPopup.ui(Str.get(this.lang, "sections"), new Runnable() {
            @Override
            public void run() {
                ReaderActivity.this.showSections();
            }
        });
        newPopup.ui(Str.get(this.lang, "addBookmark"), new Runnable() {
            @Override
            public void run() {
                ReaderActivity.this.addBookmark();
            }
        });
        newPopup.ui(Str.get(this.lang, "fontSize"), new Runnable() {
            @Override
            public void run() {
                ReaderActivity.this.showSizes();
            }
        });
        newPopup.ui(Str.get(this.lang, "toStart"), new Runnable() {
            @Override
            public void run() {
                ReaderActivity.this.jumpTo(0);
            }
        });
        newPopup.show(0);
    }

    private void showSections() {
        Popup newPopup = newPopup(Str.get(this.lang, "sections"));
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < this.vis.size(); i3++) {
            final int pos = i3;
            Doc.Item item = this.d.items.get(this.vis.get(pos).intValue());
            if (item.heading && item.level <= 2) {
                newPopup.item(null, item.plain, item.level == 1 ? 14.0f : 12.5f, item.level == 1 ? 0 : 10, new Runnable() {
                    @Override
                    public void run() {
                        jumpTo(pos);
                    }
                });
                if (pos <= this.cur) {
                    i2 = i;
                }
                i++;
            }
        }
        if (i == 0) {
            newPopup.ui(Str.get(this.lang, "toStart"), new Runnable() {
                @Override
                public void run() {
                    ReaderActivity.this.jumpTo(0);
                }
            });
        }
        newPopup.show(i2);
    }

    private void showSizes() {
        Popup newPopup = newPopup(Str.get(this.lang, "fontSize"));
        String[] strArr = {"small", "medium", "large"};
        int indexOf = Arrays.asList(Prefs.SIZES).indexOf(this.prefs.size());
        for (int i = 0; i < 3; i++) {
            final int k = i;
            newPopup.ui(Str.get(this.lang, strArr[k]), new Runnable() {
                @Override
                public void run() {
                    prefs.size(Prefs.SIZES[k]);
                    textSp = prefs.textSp();
                    final int keep = cur;
                    ReaderActivity.this.list.animate().alpha(0.0f).setDuration(90L).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            ReaderActivity.this.adapter.notifyDataSetChanged();
                            list.setSelectionFromTop(keep, margin());
                            ReaderActivity.this.list.animate().alpha(1.0f).setDuration(160L).start();
                        }
                    }).start();
                }
            });
        }
        newPopup.current(indexOf).show(Math.max(0, indexOf));
    }

    private void addBookmark() {
        String str;
        Prefs.Bookmark bookmark = new Prefs.Bookmark();
        bookmark.nusach = this.nusach;
        bookmark.doc = this.doc;
        bookmark.para = this.vis.get(this.cur).intValue();
        bookmark.from = this.from;
        bookmark.to = this.to;
        int intValue = this.vis.get(this.cur).intValue();
        while (true) {
            if (intValue < this.from) {
                str = "";
                break;
            }
            Doc.Item item = this.d.items.get(intValue);
            if (item.heading) {
                str = item.plain;
                break;
            }
            intValue--;
        }
        String stringExtra = getIntent().getStringExtra("he");
        if (stringExtra == null) {
            stringExtra = Names.docHe(this.doc);
        }
        if (!str.isEmpty() && !str.equals(stringExtra)) {
            stringExtra = stringExtra + " · " + str;
        }
        bookmark.title = stringExtra;
        this.prefs.addBookmark(bookmark);
        flash(Str.get(this.lang, "bookmarkAdded"));
    }
}
