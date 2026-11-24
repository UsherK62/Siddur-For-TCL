package com.pureflip.siddur;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {
    private ScrollView sv;



    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setMenuVisible(false);
        setHeaderUi("about");
        ScrollView scrollView = new ScrollView(this);
        this.sv = scrollView;
        scrollView.setVerticalScrollBarEnabled(false);
        this.sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 10.0f), Ui.dp(this, 8.0f), Ui.dp(this, 10.0f), Ui.dp(this, 16.0f));
        boolean rtl = Str.rtl(this.lang);
        TextView text = Ui.text(this, rtl ? "סידור · PureFlip" : "PureFlip Siddur · סידור", 16.0f, this.pal.ink, Ui.plexBold);
        if (rtl) {
            Ui.hebrew(text);
        } else {
            Ui.latin(text);
        }
        linearLayout.addView(text);
        TextView text2 = Ui.text(this, Str.get(this.lang, "version") + " " + BuildConfigVersion.NAME, 12.5f, this.pal.ink2, Ui.plex);
        if (rtl) {
            Ui.hebrew(text2);
        } else {
            Ui.latin(text2);
        }
        linearLayout.addView(text2);
        TextView text3 = Ui.text(this, Str.aboutBody(this.lang), 13.0f, this.pal.ink, Ui.plex);
        text3.setLineSpacing(0.0f, 1.1f);
        if (rtl) {
            Ui.hebrew(text3);
        } else {
            Ui.latin(text3);
        }
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(this, 8.0f);
        linearLayout.addView(text3, layoutParams);
        TextView text4 = Ui.text(this, "Ashkenaz and Sefard weekday tefillos and Birkas Hamazon: The Metsudah Siddur, 1981 (Avrohom Davis), via Sefaria, CC BY.\nAshkenaz brachos: Daat Siddur Ashkenaz (daat.ac.il), Public Domain.\nSefard brachos: Torat Emet 357, Public Domain.\nEdot HaMizrach: Siddur Edot HaMizrach, Shaliehsaboo Edition, CC0.\nTehillim: Tanach with Nikkud (tanach.us), Public Domain.\nZmanim and calendar: KosherJava Zmanim 3.0.0, LGPL 2.1.\nFonts: Frank Ruhl Libre and IBM Plex Sans Hebrew, SIL Open Font License 1.1.", 11.5f, this.pal.ink2, Ui.plex);
        text4.setLineSpacing(0.0f, 1.1f);
        Ui.latin(text4);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = Ui.dp(this, 10.0f);
        linearLayout.addView(text4, layoutParams2);
        this.sv.addView(linearLayout);
        this.body.addView(this.sv, fill());
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 20) {
            this.sv.smoothScrollBy(0, Ui.dp(this, 60.0f));
            return true;
        }
        if (i == 19) {
            this.sv.smoothScrollBy(0, -Ui.dp(this, 60.0f));
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }
}
