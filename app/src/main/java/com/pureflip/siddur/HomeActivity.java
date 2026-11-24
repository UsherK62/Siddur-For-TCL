package com.pureflip.siddur;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

public class HomeActivity extends BaseActivity {
    private String builtLayout;
    private String builtNusach;
    private View carCard;
    private TextView carEn;
    private TextView carHe;
    private int carouselIndex;
    private LinearLayout dots;
    private Luach luach;
    private int nowTef;
    private RowList rowList;
    private TextView spEn;
    private TextView spHe;
    private TextView spIns;
    private TextView spTime;
    private int focusIndex = -1;
    private int shownInfo = -1;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.focusIndex = bundle.getInt("focus", -1);
            this.carouselIndex = bundle.getInt("car", 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("focus", currentFocusIndex());
        bundle.putInt("car", this.carouselIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }
        Luach current = Luach.current(this.prefs);
        this.luach = current;
        int nowTefillah = current.nowTefillah();
        if (!this.prefs.layout().equals(this.builtLayout) || !this.prefs.nusach().equals(this.builtNusach) || nowTefillah != this.nowTef || this.builtLayout == null) {
            int currentFocusIndex = this.builtLayout == null ? this.focusIndex : currentFocusIndex();
            this.nowTef = nowTefillah;
            build(currentFocusIndex);
        }
        if (this.nowTef >= 0 && this.nowTef < Names.HOME_DOC.length && Names.HOME_DOC[this.nowTef] != null) {
            Doc.preload(this, this.prefs.nusach(), Names.HOME_DOC[this.nowTef]);
        }
        Ui.preloadSettingsFonts();
    }

    private int currentFocusIndex() {
        View focused;
        if ("carousel".equals(this.builtLayout)) {
            return this.carouselIndex;
        }
        if (this.rowList == null || (focused = focused()) == null) {
            return -1;
        }
        return this.rowList.rows.indexOf(focused);
    }

    private void build(int i) {
        this.builtLayout = this.prefs.layout();
        this.builtNusach = this.prefs.nusach();
        this.body.removeAllViews();
        this.rowList = null;
        if (Str.rtl(this.lang)) {
            setHeader(null, "סידור");
        } else {
            setHeader("Siddur", null);
        }
        this.headLeft.setVisibility(View.VISIBLE);
        if (Str.rtl(this.lang)) {
            this.headLeft.setText(Names.nusachHe(this.builtNusach));
            this.headLeft.setTypeface(Ui.plexSemi);
        } else {
            this.headRight.setText(Names.nusachHe(this.builtNusach));
        }
        switch (this.builtLayout) {
            case "toc":
                buildToc(i);
                break;
            case "carousel":
                buildCarousel(i);
                break;
            case "split":
                buildSplit(i);
                break;
            default:
                buildList(i);
                break;
        }
        fadeIn(this.body);
    }

    private String gregorian() {
        ZonedDateTime zonedDateTime = this.luach.now;
        if ("en".equals(this.lang)) {
            return new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}[zonedDateTime.getDayOfWeek().getValue() - 1] + ", " + new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}[zonedDateTime.getMonthValue() - 1] + " " + zonedDateTime.getDayOfMonth();
        }
        return Str.week(this.lang, zonedDateTime.getDayOfWeek().getValue() % 7) + " " + zonedDateTime.getMonthValue() + "/" + zonedDateTime.getDayOfMonth();
    }

    private TextView small(CharSequence charSequence, int i, boolean z, Typeface typeface) {
        TextView line = Ui.line(this, charSequence, 12.0f, i, typeface);
        if (z) {
            Ui.hebrew(line);
        } else {
            Ui.latin(line);
        }
        if (Str.rtl(this.lang) && !z) {
            line.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        return line;
    }

    private View dateBlock() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 10.0f), Ui.dp(this, 4.0f), Ui.dp(this, 10.0f), Ui.dp(this, 3.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(Gravity.CENTER_VERTICAL);
        TextView small = small(gregorian(), this.pal.ink, false, Ui.plexSemi);
        small.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        linearLayout2.addView(small, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView small2 = small(this.luach.hebrewDate(), this.pal.ink, true, Ui.plexSemi);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = Ui.dp(this, 6.0f);
        linearLayout2.addView(small2, layoutParams);
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setGravity(Gravity.TOP);
        TextView small3 = small(this.luach.city.name(this.lang), this.pal.ink2, false, Ui.plex);
        small3.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        linearLayout3.addView(small3, new LinearLayout.LayoutParams(-2, -2));
        TextView text = Ui.text(this, this.luach.insertions(this.nowTef), 11.5f, this.pal.acc, Ui.plexSemi);
        Ui.hebrew(text);
        text.setMaxLines(2);
        text.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = Ui.dp(this, 8.0f);
        linearLayout3.addView(text, layoutParams2);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = Ui.dp(this, 1.0f);
        linearLayout.addView(linearLayout3, layoutParams3);
        return linearLayout;
    }

    private void open(int i) {
        if (i < 0 || i > 6) {
            return;
        }
        if (i <= 3) {
            Intent intent = new Intent(this, ReaderActivity.class);
            intent.putExtra("doc", Names.HOME_DOC[i]);
            go(intent);
        } else {
            Intent intent2 = new Intent(this, ListActivity.class);
            intent2.putExtra("kind", i == 4 ? "brachos" : i == 5 ? "tehillim" : "bookmarks");
            go(intent2);
        }
    }

    private View homeRow(int i, float f, boolean z, boolean z2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 10.0f), 0, Ui.dp(this, 8.0f), 0);
        TextView line = Ui.line(this, Names.HOME_EN[i], f - 0.5f, this.pal.ink, Ui.plexSemi);
        Ui.latin(line);
        linearLayout.addView(line, new LinearLayout.LayoutParams(-2, -2));
        if (z2) {
            Dots dots = new Dots(this, this.pal.ink2);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 12.0f), 1.0f);
            layoutParams.leftMargin = Ui.dp(this, 4.0f);
            layoutParams.rightMargin = Ui.dp(this, 4.0f);
            linearLayout.addView(dots, layoutParams);
        } else {
            linearLayout.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
        }
        TextView line2 = Ui.line(this, Names.HOME_HE[i], 0.5f + f, this.pal.ink, Ui.plexSemi);
        Ui.hebrew(line2);
        linearLayout.addView(line2, new LinearLayout.LayoutParams(-2, -2));
        if (z) {
            TextView line3 = Ui.line(this, String.valueOf(i + 1), f - 1.0f, (i == this.nowTef && z2) ? this.pal.acc : this.pal.ink2, Ui.plexSemi);
            line3.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            linearLayout.addView(line3, new LinearLayout.LayoutParams(Ui.dp(this, 15.0f), -2));
        }
        return linearLayout;
    }

    static final class Dots extends View {
        private final Paint p;

        Dots(Context context, int i) {
            super(context);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.p = paint;
            paint.setColor(i);
            paint.setAlpha(150);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float max = Math.max(0.8f, getResources().getDisplayMetrics().density * 0.8f);
            float f = 4.2f * max;
            float height = getHeight() * 0.62f;
            for (float f2 = max; f2 < getWidth() - max; f2 += f) {
                canvas.drawCircle(f2, height, max, this.p);
            }
        }
    }

    private void focusLater(final int i) {
        RowList rowList = this.rowList;
        if (rowList == null) {
            return;
        }
        rowList.scroll.post(new Runnable() {
            @Override
            public void run() {
                HomeActivity.this.rowList.focus(i);
            }
        });
    }

    private void buildList(int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(dateBlock());
        this.rowList = new RowList(this, this.pal, false, 7, 2, 3);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout2.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout2.setBaselineAligned(true);
        linearLayout2.setPadding(Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f), 0);
        linearLayout2.setBackground(Ui.cardBg(this, this.pal, true));
        // The Now card scrolls a name that doesn't fit, focused or not ("Now · Shacharis" next to the Hebrew).
        TextView line = Ui.line(this, "Now · " + Names.HOME_EN[this.nowTef], 15.0f, this.pal.ink, Ui.plexBold).always();
        Ui.latin(line);
        linearLayout2.addView(line, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView line2 = Ui.line(this, "עכשיו · " + Names.HOME_HE[this.nowTef], 16.0f, this.pal.ink, Ui.plexBold).always();
        Ui.hebrew(line2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = Ui.dp(this, 6.0f);
        linearLayout2.addView(line2, layoutParams);
        this.rowList.addRow(linearLayout2, 34, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity homeActivity = HomeActivity.this;
                homeActivity.open(homeActivity.nowTef);
            }
        });
        for (int i2 = 0; i2 < 7; i2++) {
            final int k = i2;
            View homeRow = homeRow(k, 14.0f, true, false);
            homeRow.setBackground(Ui.cardBg(this, this.pal, true));
            this.rowList.addRow(homeRow, 27, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    HomeActivity.this.open(k);
                }
            });
        }
        linearLayout.addView(this.rowList.view(), new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.body.addView(linearLayout, fill());
        if (i < 0) {
            i = 0;
        }
        focusLater(i);
    }

    private void buildToc(int i) {
        this.rowList = new RowList(this, this.pal, true, 7, 6, 2);
        for (int i2 = 0; i2 < 7; i2++) {
            final int k = i2;
            View homeRow = homeRow(k, 14.0f, true, true);
            homeRow.setBackground(Ui.cardBg(this, this.pal, false));
            this.rowList.addRow(homeRow, 28, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    HomeActivity.this.open(k);
                }
            });
        }
        this.body.addView(this.rowList.view(), fill());
        if (i < 0) {
            i = this.nowTef;
        }
        focusLater(i);
    }

    private String hm(Instant instant) {
        if (instant == null) {
            return "";
        }
        ZonedDateTime atZone = instant.atZone(ZoneId.of(this.luach.city.tz));
        int hour = atZone.getHour() % 12;
        return String.format(Locale.US, "%d:%02d", Integer.valueOf(hour != 0 ? hour : 12), Integer.valueOf(atZone.getMinute()));
    }

    private String timeRange(int i) {
        if (i > 2) {
            return "";
        }
        ComprehensiveZmanimCalendar comprehensiveZmanimCalendar = new ComprehensiveZmanimCalendar(new GeoLocation(this.luach.city.en, this.luach.city.lat, this.luach.city.lon, this.luach.city.elev, ZoneId.of(this.luach.city.tz)));
        comprehensiveZmanimCalendar.setLocalDate(this.luach.now.toLocalDate());
        if (i == 0) {
            return hm(comprehensiveZmanimCalendar.getAlos72Minutes()) + " – " + hm(comprehensiveZmanimCalendar.getChatzosHayom());
        }
        if (i == 1) {
            return hm(comprehensiveZmanimCalendar.getMinchaGedolaGRA()) + " – " + hm(comprehensiveZmanimCalendar.getSunset());
        }
        return hm(comprehensiveZmanimCalendar.getTzaisGeonim8Point5Degrees()) + " – " + hm(comprehensiveZmanimCalendar.getSolarMidnight());
    }

    private void buildSplit(int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(Ui.dp(this, 6.0f), Ui.dp(this, 6.0f), Ui.dp(this, 6.0f), Ui.dp(this, 6.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        linearLayout2.setPadding(Ui.dp(this, 8.0f), Ui.dp(this, 8.0f), Ui.dp(this, 8.0f), Ui.dp(this, 8.0f));
        linearLayout2.setBackground(Ui.round(this, this.pal.card, this.pal.radius, this.pal.line, 1.0f));
        TextView line = Ui.line(this, "", 27.0f, this.pal.ink, Ui.frankBold);
        this.spHe = line;
        Ui.hebrew(line);
        linearLayout2.addView(this.spHe, new LinearLayout.LayoutParams(-1, -2));
        TextView line2 = Ui.line(this, "", 13.0f, this.pal.acc, Ui.plexSemi);
        this.spEn = line2;
        Ui.latin(line2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(this, 2.0f);
        linearLayout2.addView(this.spEn, layoutParams);
        View view = new View(this);
        view.setBackgroundColor(this.pal.line);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, 1);
        layoutParams2.topMargin = Ui.dp(this, 6.0f);
        layoutParams2.bottomMargin = Ui.dp(this, 6.0f);
        linearLayout2.addView(view, layoutParams2);
        TextView line3 = Ui.line(this, "", 13.0f, this.pal.ink, Ui.plexBold);
        this.spTime = line3;
        Ui.latin(line3);
        linearLayout2.addView(this.spTime, new LinearLayout.LayoutParams(-1, -2));
        TextView line4 = Ui.line(this, gregorian(), 11.5f, this.pal.ink2, Ui.plex);
        Ui.latin(line4);
        if (Str.rtl(this.lang)) {
            line4.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = Ui.dp(this, 4.0f);
        linearLayout2.addView(line4, layoutParams3);
        TextView line5 = Ui.line(this, this.luach.hebrewDate(), 11.5f, this.pal.ink2, Ui.plex);
        Ui.hebrew(line5);
        linearLayout2.addView(line5, new LinearLayout.LayoutParams(-1, -2));
        TextView line6 = Ui.line(this, this.luach.city.name(this.lang), 11.5f, this.pal.ink2, Ui.plex);
        Ui.latin(line6);
        linearLayout2.addView(line6, new LinearLayout.LayoutParams(-1, -2));
        TextView text = Ui.text(this, "", 11.5f, this.pal.acc, Ui.plexSemi);
        this.spIns = text;
        Ui.hebrew(text);
        this.spIns.setMaxLines(4);
        this.spIns.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = Ui.dp(this, 4.0f);
        linearLayout2.addView(this.spIns, layoutParams4);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -1, 1.08f));
        RowList rowList = new RowList(this, this.pal, true, 4, 4, 1);
        this.rowList = rowList;
        rowList.scroll.setBackground(Ui.round(this, this.pal.card, this.pal.radius, this.pal.line, 1.0f));
        for (int i2 = 0; i2 < 7; i2++) {
            final int k = i2;
            TextView line7 = Ui.line(this, Names.HOME_HE[k], 14.5f, this.pal.ink, Ui.plexSemi);
            Ui.hebrew(line7);
            line7.setPadding(Ui.dp(this, 8.0f), 0, Ui.dp(this, 8.0f), 0);
            line7.setBackground(Ui.cardBg(this, this.pal, false));
            this.rowList.addRow(line7, 26, new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    HomeActivity.this.open(k);
                }
            });
            line7.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view2, boolean z) {
                    if (z) {
                        HomeActivity.this.rowList.ring.moveTo(view2, true);
                        HomeActivity.this.showInfo(k, true);
                    }
                }
            });
        }
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(0, -1, 0.92f);
        layoutParams5.leftMargin = Ui.dp(this, 6.0f);
        linearLayout.addView(this.rowList.view(), layoutParams5);
        this.body.addView(linearLayout, fill());
        int i3 = i >= 0 ? i : this.nowTef;
        showInfo(i3, false);
        focusLater(i3);
    }

    /** Fills the split layout's info panel for row {@code i}; fades it when {@code animate}. */
    private void showInfo(final int i, boolean animate) {
        if (i == this.shownInfo || this.spHe == null) {
            return;
        }
        this.shownInfo = i;
        final Runnable set = new Runnable() {
            @Override
            public void run() {
                spHe.setText(Names.HOME_HE[i]);
                spEn.setText(Names.HOME_EN[i]);
                String range = timeRange(i);
                spTime.setText(range);
                spTime.setVisibility(range.isEmpty() ? View.GONE : View.VISIBLE);
                spIns.setText(i <= 2 ? luach.insertions(i) : "");
            }
        };
        final View panel = (View) this.spHe.getParent();
        if (!animate) {
            set.run();
            return;
        }
        panel.animate().cancel();
        panel.animate().alpha(0.15f).setDuration(80L).withEndAction(new Runnable() {
            @Override
            public void run() {
                set.run();
                panel.animate().alpha(1.0f).setDuration(140L).start();
            }
        }).start();
    }

    private void buildCarousel(int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(dateBlock());
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackground(Ui.cardBg(this, this.pal, true));
        frameLayout.setFocusable(true);
        frameLayout.setClickable(true);
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity homeActivity = HomeActivity.this;
                homeActivity.open(homeActivity.carouselIndex);
            }
        });
        this.carCard = frameLayout;
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        linearLayout2.setGravity(Gravity.CENTER);
        TextView line = Ui.line(this, "", 40.0f, this.pal.ink, Ui.frankBold);
        this.carHe = line;
        line.setGravity(Gravity.CENTER);
        this.carHe.setTextDirection(View.TEXT_DIRECTION_RTL);
        linearLayout2.addView(this.carHe, new LinearLayout.LayoutParams(-1, -2));
        TextView line2 = Ui.line(this, "", 14.0f, this.pal.ink2, Ui.plexSemi);
        this.carEn = line2;
        line2.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(this, 2.0f);
        linearLayout2.addView(this.carEn, layoutParams);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-1, -2, 17);
        int dp = Ui.dp(this, 26.0f);
        layoutParams2.rightMargin = dp;
        layoutParams2.leftMargin = dp;
        frameLayout.addView(linearLayout2, layoutParams2);
        View line3 = Ui.line(this, "◀", 13.0f, this.pal.ink2, Ui.plex);
        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(-2, -2, 19);
        layoutParams3.leftMargin = Ui.dp(this, 9.0f);
        frameLayout.addView(line3, layoutParams3);
        View line4 = Ui.line(this, "▶", 13.0f, this.pal.ink2, Ui.plex);
        FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(-2, -2, 21);
        layoutParams4.rightMargin = Ui.dp(this, 9.0f);
        frameLayout.addView(line4, layoutParams4);
        LinearLayout linearLayout3 = new LinearLayout(this);
        this.dots = linearLayout3;
        linearLayout3.setOrientation(LinearLayout.HORIZONTAL);
        this.dots.setGravity(Gravity.CENTER);
        for (int i2 = 6; i2 >= 0; i2--) {
            View view = new View(this);
            LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(Ui.dp(this, 7.0f), Ui.dp(this, 7.0f));
            int dp2 = Ui.dp(this, 3.0f);
            layoutParams5.rightMargin = dp2;
            layoutParams5.leftMargin = dp2;
            this.dots.addView(view, layoutParams5);
        }
        FrameLayout.LayoutParams layoutParams6 = new FrameLayout.LayoutParams(-2, -2, 81);
        layoutParams6.bottomMargin = Ui.dp(this, 12.0f);
        frameLayout.addView(this.dots, layoutParams6);
        FrameLayout frameLayout2 = new FrameLayout(this);
        frameLayout2.setPadding(Ui.dp(this, 7.0f), Ui.dp(this, 2.0f), Ui.dp(this, 7.0f), Ui.dp(this, 7.0f));
        frameLayout2.addView(frameLayout, new FrameLayout.LayoutParams(-1, -1));
        final FocusRing ring = new FocusRing(this, this.pal);
        frameLayout2.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view2, boolean z) {
                if (z) {
                    ring.moveTo(view2, false);
                } else {
                    ring.hide();
                }
            }
        });
        frameLayout2.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
                if (HomeActivity.this.carCard.hasFocus()) {
                    ring.resnap();
                }
            }
        });
        linearLayout.addView(frameLayout2, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.body.addView(linearLayout, fill());
        if (i < 0) {
            i = this.nowTef;
        }
        this.carouselIndex = i;
        setCarousel(i, 0);
        frameLayout.post(new Runnable() {
            @Override
            public void run() {
                HomeActivity.this.carCard.requestFocus();
            }
        });
    }

    /** Shows item {@code i} in the carousel; {@code dir} is the slide direction (0 = no animation). */
    private void setCarousel(int i, int dir) {
        final int idx = (i + 7) % 7;
        this.carouselIndex = idx;
        for (int j = 0; j < 7; j++) {
            View dot = this.dots.getChildAt(6 - j); // dots run right to left
            boolean on = j == idx;
            dot.setBackground(Ui.round(this, on ? this.pal.acc : 0, 4.0f, on ? this.pal.acc : this.pal.ink2, on ? 0.0f : 1.0f));
            dot.animate().scaleX(on ? 1.25f : 1.0f).scaleY(on ? 1.25f : 1.0f).setDuration(180L).start();
        }
        if (dir == 0) {
            this.carHe.setText(Names.HOME_HE[idx]);
            this.carEn.setText(Names.HOME_EN[idx]);
            return;
        }
        final View c = (View) this.carHe.getParent();
        final float dx = Ui.dp(this, 36.0f) * dir;
        c.animate().cancel();
        c.animate().translationX(dx).alpha(0.0f).setDuration(100L).withEndAction(new Runnable() {
            @Override
            public void run() {
                carHe.setText(Names.HOME_HE[idx]);
                carEn.setText(Names.HOME_EN[idx]);
                c.setTranslationX(-dx);
                c.animate().translationX(0.0f).alpha(1.0f).setDuration(150L).setInterpolator(new DecelerateInterpolator()).start();
            }
        }).start();
    }

    @Override
    protected void onMenuKey() {
        go(SettingsActivity.class);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if ("carousel".equals(this.builtLayout)) {
            if (i == 21) {
                setCarousel(this.carouselIndex + 1, 1);
                return true;
            }
            if (i == 22) {
                setCarousel(this.carouselIndex - 1, -1);
                return true;
            }
            if (i == 19 || i == 20) {
                return true;
            }
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i >= 8 && i <= 14) {
            open(i - 8);
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public void onBackPressed() {
        if (this.popup == null || !this.popup.isShowing()) {
            finish();
        } else {
            this.popup.dismiss();
        }
    }
}
