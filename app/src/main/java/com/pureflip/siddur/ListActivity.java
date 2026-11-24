package com.pureflip.siddur;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class ListActivity extends BaseActivity {
    private String kind;
    private RowList rl;
    private int typed = 0;
    private final Handler h = new Handler(Looper.getMainLooper());
    private int focusKeep = -1;



    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String stringExtra = getIntent().getStringExtra("kind");
        this.kind = stringExtra;
        if (stringExtra == null) {
            this.kind = "brachos";
        }
        if (bundle != null) {
            this.focusKeep = bundle.getInt("focus", -1);
        }
        if (this.kind.startsWith("tehillim")) {
            // Tehillim is the biggest text (all 150 chapters): parse it while the list is on screen.
            Doc.preload(this, this.prefs.nusach(), "tehillim");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        View focused = focused();
        RowList rowList = this.rl;
        if (rowList == null || focused == null) {
            return;
        }
        bundle.putInt("focus", rowList.rows.indexOf(focused));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }
        if (this.rl == null || "bookmarks".equals(this.kind)) {
            int i = this.focusKeep;
            View focused = focused();
            RowList rowList = this.rl;
            if (rowList != null && focused != null) {
                i = rowList.rows.indexOf(focused);
            }
            build(i);
            this.focusKeep = -1;
        }
    }

    private View row(CharSequence charSequence, CharSequence charSequence2, float f) {
        LinearLayout twoSide = twoSide(charSequence, charSequence2, f, this.pal.ink, this.pal.ink, Ui.plexSemi, Ui.plexSemi);
        twoSide.setPadding(Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f), 0);
        twoSide.setBackground(Ui.cardBg(this, this.pal, true));
        return twoSide;
    }

    private View uiRow(String str, String str2) {
        return Str.rtl(this.lang) ? row(str2, str, 14.0f) : row(str, str2, 14.0f);
    }

    private void build(int keep) {
        this.body.removeAllViews();
        setMenuVisible(false);
        this.rl = new RowList(this, this.pal, false, 7, 6, 3);
        Luach luach = Luach.current(this.prefs);
        int focus = 0;
        switch (this.kind) {
            case "brachos":
                setHeader(Str.rtl(this.lang) ? null : "Brachos", "ברכות");
                for (int i = 0; i < Names.BRACHOS_DOC.length; i++) {
                    final int k = i;
                    this.rl.addRow(row(Str.rtl(this.lang) ? null : Str.get("en", Names.BRACHOS_DOC[k]), Names.BRACHOS_HE[k], 12.5f), 28, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openDoc(Names.BRACHOS_DOC[k], Names.BRACHOS_HE[k], Str.get("en", Names.BRACHOS_DOC[k]));
                        }
                    });
                }
                break;
            case "tehillim": {
                setHeader(Str.rtl(this.lang) ? null : "Tehillim", "תהילים");
                final int dm = luach.hebrewDayOfMonth();
                this.rl.addRow(uiRow(Str.get(this.lang, "todayTehillim"), "יום " + Ui.gem(dm)), 30, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMonthDay(dm);
                    }
                });
                String[] labels = {"byMonth", "byWeek", "byPerek"};
                final String[] kinds = {"tehillim_month", "tehillim_week", "tehillim_perek"};
                for (int i = 0; i < 3; i++) {
                    final int j = i;
                    this.rl.addRow(uiRow(Str.get(this.lang, labels[j]), null), 30, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(ListActivity.this, ListActivity.class);
                            intent.putExtra("kind", kinds[j]);
                            go(intent);
                        }
                    });
                }
                break;
            }
            case "tehillim_month":
                setHeader(Str.rtl(this.lang) ? null : Str.get("en", "byMonth"), Str.rtl(this.lang) ? Str.get(this.lang, "byMonth") : "תהילים");
                for (int i = 0; i < 30; i++) {
                    final int day = i + 1;
                    String range;
                    if (Names.MONTH_START[i] == Names.MONTH_END[i]) {
                        // Days 25 and 26 split kapitel 119.
                        range = day == 25 ? "119:1–96" : "119:97–176";
                    } else {
                        range = Names.MONTH_START[i] + "–" + Names.MONTH_END[i];
                    }
                    this.rl.addRow(row(range, "יום " + Ui.gem(day), 13.5f), 27, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openMonthDay(day);
                        }
                    });
                }
                focus = luach.hebrewDayOfMonth() - 1;
                break;
            case "tehillim_week":
                setHeader(Str.rtl(this.lang) ? null : Str.get("en", "byWeek"), Str.rtl(this.lang) ? Str.get(this.lang, "byWeek") : "תהילים");
                for (int i = 0; i < 7; i++) {
                    final int k2 = i;
                    this.rl.addRow(uiRow(Str.week(this.lang, k2), Names.WEEK_START[k2] + "–" + Names.WEEK_END[k2]), 27, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openTehillim(Names.WEEK_START[k2], Names.WEEK_END[k2], 0, 0, "תהילים · " + Str.WEEK_HE[k2]);
                        }
                    });
                }
                focus = luach.hebrewDow();
                break;
            case "tehillim_perek":
                setHeader(Str.rtl(this.lang) ? null : Str.get("en", "byPerek"), Str.rtl(this.lang) ? Str.get(this.lang, "byPerek") : "תהילים");
                for (int i = 1; i <= 150; i++) {
                    final int ch = i;
                    this.rl.addRow(row(String.valueOf(ch), "מזמור " + Ui.gem(ch), 13.5f), 26, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openTehillim(ch, ch, 0, 0, "תהילים " + Ui.gem(ch));
                        }
                    });
                }
                break;
            case "bookmarks": {
                setHeader(Str.rtl(this.lang) ? null : "Bookmarks", "סימניות");
                List<Prefs.Bookmark> list = this.prefs.bookmarks();
                setMenuVisible(!list.isEmpty());
                if (list.isEmpty()) {
                    TextView empty = Ui.text(this, Str.get(this.lang, "noBookmarks"), 14.0f, this.pal.ink2, Ui.plexSemi);
                    empty.setGravity(Gravity.CENTER);
                    if (Str.rtl(this.lang)) {
                        empty.setTextDirection(View.TEXT_DIRECTION_RTL);
                    }
                    this.body.addView(empty, fill());
                    fadeIn(empty);
                    return;
                }
                for (final Prefs.Bookmark b : list) {
                    this.rl.addRow(row(Str.rtl(this.lang) ? null : Names.docEn(b.doc), b.title, 13.0f), 28, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(ListActivity.this, ReaderActivity.class);
                            intent.putExtra("doc", b.doc);
                            intent.putExtra("nusach", b.nusach);
                            intent.putExtra("para", b.para);
                            intent.putExtra("from", b.from);
                            intent.putExtra("to", b.to);
                            go(intent);
                        }
                    });
                }
                break;
            }
        }
        this.body.addView(this.rl.view(), fill());
        final int f = keep >= 0 ? keep : focus;
        this.rl.scroll.post(new Runnable() {
            @Override
            public void run() {
                rl.focus(Math.max(0, Math.min(f, rl.rows.size() - 1)));
            }
        });
    }

    private void openDoc(String str, String str2, String str3) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("doc", str);
        intent.putExtra("he", str2);
        intent.putExtra("en", str3);
        go(intent);
    }

    private void openMonthDay(int i) {
        int max = Math.max(1, Math.min(30, i)) - 1;
        if (i == 25) {
            openTehillim(119, 119, 1, 96, "תהילים · יום " + Ui.gem(i));
        } else if (i == 26) {
            openTehillim(119, 119, 97, 176, "תהילים · יום " + Ui.gem(i));
        } else {
            openTehillim(Names.MONTH_START[max], Names.MONTH_END[max], 0, 0, "תהילים · יום " + Ui.gem(i));
        }
    }

    private void openTehillim(int i, int i2, int i3, int i4, String str) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("doc", "tehillim");
        intent.putExtra("he", str);
        intent.putExtra("en", "Tehillim");
        intent.putExtra("chFrom", i);
        intent.putExtra("chTo", i2);
        if (i3 > 0) {
            intent.putExtra("vFrom", i3);
            intent.putExtra("vTo", i4);
        }
        go(intent);
    }

    @Override
    protected void onMenuKey() {
        if (!"bookmarks".equals(this.kind) || this.rl == null) {
            return;
        }
        View focused = focused();
        final int i = focused == null ? -1 : this.rl.rows.indexOf(focused);
        if (i < 0) {
            return;
        }
        Popup newPopup = newPopup(null);
        newPopup.ui(Str.get(this.lang, "deleteBookmark"), new Runnable() {
            @Override
            public void run() {
                List<Prefs.Bookmark> list = prefs.bookmarks();
                if (i < list.size()) {
                    list.remove(i);
                    prefs.saveBookmarks(list);
                }
                build(Math.max(0, i - 1));
            }
        });
        newPopup.ui(Str.get(this.lang, "cancel"), null);
        newPopup.show(0);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i >= 7 && i <= 16 && this.rl != null) {
            int i2 = i - 7;
            if ("tehillim_perek".equals(this.kind) || "tehillim_month".equals(this.kind)) {
                int i3 = (this.typed * 10) + i2;
                this.typed = i3;
                if (i3 > this.rl.rows.size()) {
                    this.typed = i2;
                }
                int i4 = this.typed;
                if (i4 >= 1) {
                    this.rl.focus(i4 - 1);
                }
                this.h.removeCallbacksAndMessages(null);
                this.h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ListActivity.this.typed = 0;
                    }
                }, 1200L);
                return true;
            }
            if (i2 >= 1 && i2 <= this.rl.rows.size()) {
                this.rl.rows.get(i - 8).performClick();
                return true;
            }
        }
        return super.onKeyUp(i, keyEvent);
    }
}
