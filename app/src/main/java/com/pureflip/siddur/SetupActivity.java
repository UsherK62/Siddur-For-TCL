package com.pureflip.siddur;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;

public class SetupActivity extends BaseActivity {
    private RowList list;
    private int step;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setMenuVisible(false);
        this.step = bundle != null ? bundle.getInt("step", 0) : 0;
        build();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("step", this.step);
    }

    private void build() {
        this.body.removeAllViews();
        this.lang = this.prefs.lang();
        this.barRight.setText(Str.get(this.lang, "back"));
        this.barLeft.setText(Str.get(this.lang, "menu"));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (this.step == 0) {
            setHeader("Siddur", "סידור");
        } else {
            setHeaderUi("nusach");
            TextView line = Ui.line(this, Str.get(this.lang, "whichNusach"), 13.5f, this.pal.ink2, Ui.plexSemi);
            if (Str.rtl(this.lang)) {
                Ui.hebrew(line);
            } else {
                Ui.latin(line);
            }
            line.setPadding(Ui.dp(this, 10.0f), Ui.dp(this, 5.0f), Ui.dp(this, 10.0f), 0);
            linearLayout.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }
        this.list = new RowList(this, this.pal, true, 8, 6, 6);
        if (this.step == 0) {
            // Prefs.LANGS order: English, Hebrew.
            String[] strArr = {"English", "Hebrew"};
            String[] strArr2 = {null, "עברית"};
            for (int i = 0; i < Prefs.LANGS.length; i++) {
                final int k = i;
                this.list.addRow(card(strArr[k], strArr2[k]), 40, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SetupActivity.this.prefs.lang(Prefs.LANGS[k]);
                        SetupActivity setupActivity = SetupActivity.this;
                        setupActivity.lang = setupActivity.prefs.lang();
                        SetupActivity.this.step = 1;
                        SetupActivity.this.swap();
                    }
                });
            }
        } else {
            boolean rtl = Str.rtl(this.lang);
            for (int i2 = 0; i2 < 3; i2++) {
                final int k = i2;
                this.list.addRow(card(rtl ? null : Names.NUSACH_EN[k], Names.NUSACH_HE[k]), 40, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SetupActivity.this.prefs.nusach(Prefs.NUSACHOS[k]);
                        SetupActivity.this.prefs.setSetupDone();
                        SetupActivity.this.go(new Intent(SetupActivity.this, HomeActivity.class));
                        SetupActivity.this.finish();
                    }
                });
            }
        }
        linearLayout.addView(this.list.view(), new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.body.addView(linearLayout, fill());
        final int ff = this.step == 0
                ? Math.max(0, Arrays.asList(Prefs.LANGS).indexOf(this.prefs.lang()))
                : Names.nusachIndex(this.prefs.nusach());
        this.list.scroll.post(new Runnable() {
            @Override
            public void run() {
                list.focus(ff);
            }
        });
    }

    private void swap() {
        this.body.animate().alpha(0.0f).translationX(Ui.dp(this, 16.0f)).setDuration(110L).withEndAction(new Runnable() {
            @Override
            public void run() {
                SetupActivity.this.build();
                SetupActivity.this.body.setTranslationX(-Ui.dp(SetupActivity.this, 16.0f));
                SetupActivity.this.body.animate().alpha(1.0f).translationX(0.0f).setDuration(160L).start();
            }
        }).start();
    }

    private View card(String str, String str2) {
        LinearLayout twoSide = twoSide(str, str2, 17.5f, this.pal.ink, this.pal.ink, Ui.plexSemi, Ui.plexSemi);
        twoSide.setPadding(Ui.dp(this, 14.0f), 0, Ui.dp(this, 14.0f), 0);
        twoSide.setBackground(Ui.cardBg(this, this.pal, true));
        return twoSide;
    }

    @Override
    public void onBackPressed() {
        if (this.step == 1) {
            this.step = 0;
            swap();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i >= 8 && i <= 10) {
            int i2 = i - 8;
            if (i2 >= this.list.rows.size()) {
                return true;
            }
            this.list.rows.get(i2).performClick();
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }
}
