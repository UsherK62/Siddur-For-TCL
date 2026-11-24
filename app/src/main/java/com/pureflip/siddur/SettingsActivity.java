package com.pureflip.siddur;

import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings, design D1: Auto Reply's settings style mirrored for RTL. One rounded card per section under a
 * bilingual header (Hebrew right, English left). The focused row grows 25 -> 34 dp in 120 ms with a 1.5 dp
 * accent ring over a soft gradient; the dividers next to it hide. The list scrolls with {@link SmoothScroll}
 * (180 ms, Decelerate 1.6) aiming at where the focused row ends up, plus 4 dp. Pickers are
 * {@link ChoicePopup}s; the "In Israel" switch flips on OK. No hint text.
 */
public class SettingsActivity extends BaseActivity {
    static final float ROW_DP = 25.0f;
    static final float OPEN_DP = 34.0f;
    static final float HEAD_DP = 17.0f;
    private static final float CARD_PAD_DP = 3.0f;

    private int keep = 0;
    private SmoothScroll scroll;
    private LinearLayout content;
    /** Focusable rows in order. */
    final List<SetRow> rows = new ArrayList<>();
    /** Divider below row i (null for the last row of a card). */
    private final List<View> dividers = new ArrayList<>();
    /** True for the first row of each card (its header is kept in view with it). */
    private final List<Boolean> firstInCard = new ArrayList<>();
    private ChoicePopup choice;
    private SetRow lastOpen;
    private Marks.Toggle israelToggle;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setMenuVisible(false);
        if (bundle != null) {
            this.keep = bundle.getInt("focus", 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        int at = this.rows.indexOf(focused());
        bundle.putInt("focus", at >= 0 ? at : this.keep);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }
        int at = this.rows.indexOf(focused());
        if (at >= 0) {
            this.keep = at;
        }
        if (this.choice == null || !this.choice.isShowing()) {
            build();
        }
    }

    // ------------------------------------------------------------------ build

    private void build() {
        this.body.removeAllViews();
        setHeaderUi("settings");
        this.rows.clear();
        this.dividers.clear();
        this.firstInCard.clear();
        this.lastOpen = null;
        this.scroll = new SmoothScroll(this);
        this.content = new LinearLayout(this);
        this.content.setOrientation(LinearLayout.VERTICAL);
        // 7 dp side margins (card shadow room), 6 dp above the first header, 7 dp below the last card.
        this.content.setPadding(Ui.dp(this, 7.0f), Ui.dp(this, 6.0f), Ui.dp(this, 7.0f), Ui.dp(this, 7.0f));
        this.content.setClipToPadding(false);
        this.scroll.setClipToPadding(false);
        this.scroll.addView(this.content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout c = section("תצוגה", "Display", false);
        add(c, "layout", text(Str.get(this.lang, "l_" + this.prefs.layout())), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickLayout();
            }
        });
        add(c, "theme", null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTheme();
            }
        });
        themeValue(this.rows.get(this.rows.size() - 1));
        add(c, "fontSize", text(Str.get(this.lang, sizeKey(this.prefs.size()))), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickSize();
            }
        });

        c = section("סידור", "Siddur", true);
        add(c, "nusach", text(nusachName(this.prefs.nusach())), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickNusach();
            }
        });
        add(c, "city", text(Cities.of(this.prefs.city()).name(this.lang)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickCity();
            }
        });
        this.israelToggle = new Marks.Toggle(this, this.pal, this.prefs.israel());
        add(c, "israel", this.israelToggle, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipIsrael();
            }
        });

        c = section("כללי", "General", true);
        add(c, "language", text(langName(this.prefs.lang())), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickLang();
            }
        });
        add(c, "about", new Marks.Chevron(this, this.pal.ink2, Str.rtl(this.lang)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(AboutActivity.class);
            }
        });
        removeTrailingDivider(this.rows.size() - 1);

        this.body.addView(this.scroll, fill());
        final int f = Math.max(0, Math.min(this.keep, this.rows.size() - 1));
        // Restore focus without animation: open the row now so the first layout already has its final size.
        this.rows.get(f).setOpen(true, false);
        this.lastOpen = this.rows.get(f);
        updateDividers(false);
        this.scroll.post(new Runnable() {
            @Override
            public void run() {
                SetRow r = rows.get(f);
                r.requestFocus();
                reveal(r, false);
            }
        });
    }

    /** Bilingual header + an empty card; returns the card. */
    private LinearLayout section(String he, String en, boolean gapAbove) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(this, 3.0f), 0, Ui.dp(this, 3.0f), 0);
        TextView e = Ui.line(this, en, 10.5f, this.pal.head, Ui.face("en", 700));
        Ui.latin(e);
        TextView h = Ui.line(this, he, 12.0f, this.pal.head, Ui.face("he", 700));
        Ui.hebrew(h);
        head.addView(e, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        head.addView(h, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, HEAD_DP));
        if (gapAbove) {
            hl.topMargin = Ui.dp(this, 9.0f);
        }
        head.setTag("head:" + en);
        this.content.addView(head, hl);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, CARD_PAD_DP);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(this.pal.card);
        bg.setCornerRadius(Ui.dp(this, 11.0f));
        card.setBackground(bg);
        // Card shadow: a soft 1.5 dp elevation (visible on light themes, subtle on dark ones).
        card.setElevation(Ui.dp(this, 1.5f));
        card.setTag("card:" + en);
        this.content.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private TextView text(String s) {
        TextView t = Ui.line(this, s, 12.5f, this.pal.ink2, Ui.face(this.lang, 500));
        if (Str.rtl(this.lang)) {
            t.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMaxWidth(Ui.dp(this, 112.0f));
        return t;
    }

    private void themeValue(SetRow row) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(Str.get(this.lang, "t_" + this.pal.id));
        Marks.Swatches sw = new Marks.Swatches(this, this.pal.swatches, swatchEdge(), 8.0f);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gap.leftMargin = Ui.dp(this, 4.0f);
        // Swatches sit on the outer side, name next to the label.
        box.addView(sw);
        box.addView(name, gap);
        row.setValue(box, name);
    }

    private int swatchEdge() {
        return this.pal.dark ? 0x66FFFFFF : 0x40000000;
    }

    private void add(LinearLayout card, String key, View value, View.OnClickListener click) {
        final SetRow row = new SetRow(this, this.pal, this.lang, key, Str.get(this.lang, key), ROW_DP, OPEN_DP, 13.5f);
        if (value != null) {
            row.setValue(value);
        }
        row.setOnClickListener(click);
        boolean first = card.getChildCount() == 0;
        card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, ROW_DP)));
        View div = new View(this);
        div.setBackgroundColor(this.pal.line);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 1.0f)));
        int inset = Ui.dp(this, 9.0f);
        dl.leftMargin = inset;
        dl.rightMargin = inset;
        card.addView(div, dl);
        // The previous card's trailing divider must go: a card ends where the next section() begins.
        if (first && !this.rows.isEmpty()) {
            removeTrailingDivider(this.rows.size() - 1);
        }
        this.rows.add(row);
        this.dividers.add(div);
        this.firstInCard.add(first);
        final int index = this.rows.size() - 1;
        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                if (has) {
                    onRowFocus(row);
                }
            }
        });
        row.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int code, KeyEvent e) {
                if (e.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                int n = rows.size();
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusRow(index + 1 < n ? index + 1 : (e.getRepeatCount() == 0 ? 0 : index));
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_UP) {
                    focusRow(index > 0 ? index - 1 : (e.getRepeatCount() == 0 ? n - 1 : index));
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return true;
                }
                return false;
            }
        });
    }

    private void removeTrailingDivider(int rowIndex) {
        View d = this.dividers.get(rowIndex);
        if (d != null && d.getParent() instanceof ViewGroup) {
            ((ViewGroup) d.getParent()).removeView(d);
        }
        this.dividers.set(rowIndex, null);
    }

    void focusRow(int i) {
        if (i >= 0 && i < this.rows.size()) {
            this.rows.get(i).requestFocus();
        }
    }

    private void onRowFocus(SetRow row) {
        SetRow prev = this.lastOpen;
        int prevHeight = prev != null && prev != row ? prev.getHeight() : 0;
        if (prev != null && prev != row) {
            prev.setOpen(false, true);
        }
        if (!row.isOpen()) {
            row.setOpen(true, true);
        }
        this.lastOpen = row;
        this.keep = this.rows.indexOf(row);
        updateDividers(true);
        revealAfterSwap(row, prev, prevHeight, true);
    }

    /** Hides the dividers touching the open row (120 ms fade), shows the rest. */
    private void updateDividers(boolean animate) {
        int open = this.lastOpen == null ? -1 : this.rows.indexOf(this.lastOpen);
        for (int i = 0; i < this.dividers.size(); i++) {
            View d = this.dividers.get(i);
            if (d == null) {
                continue;
            }
            boolean hide = i == open || i + 1 == open;
            float to = hide ? 0.0f : 1.0f;
            d.animate().cancel();
            if (animate) {
                d.animate().alpha(to).setDuration(SetRow.EXPAND_MS).start();
            } else {
                d.setAlpha(to);
            }
        }
    }

    /** True if divider {@code i} (below row i) is currently hidden or fading out. */
    boolean dividerHidden(int i) {
        View d = i < this.dividers.size() ? this.dividers.get(i) : null;
        return d == null || d.getAlpha() < 0.5f;
    }

    View divider(int i) {
        return this.dividers.get(i);
    }

    private void reveal(SetRow row, boolean animate) {
        revealAfterSwap(row, null, 0, animate);
    }

    /**
     * Scrolls so that {@code row} plus 4 dp is in view, measured from where everything ends up once the
     * expand / collapse animations finish (not from the current, half-animated layout).
     */
    private void revealAfterSwap(SetRow row, SetRow prev, int prevHeightNow, boolean animate) {
        int vh = this.scroll.getHeight();
        if (vh == 0 || row.getParent() == null) {
            return;
        }
        Rect r = new Rect(0, 0, row.getWidth(), row.getHeight());
        try {
            this.content.offsetDescendantRectToMyCoords(row, r);
        } catch (IllegalArgumentException e) {
            return;
        }
        int index = this.rows.indexOf(row);
        // Every row that is still growing / shrinking (on key repeat there can be several) moves the rows
        // below it by (end height - laid-out height). Sum that up so the target is where things settle.
        int above = 0;
        int all = 0;
        for (int i = 0; i < this.rows.size(); i++) {
            SetRow s = this.rows.get(i);
            int d = s.endHeight() - s.getHeight();
            all += d;
            if (i < index) {
                above += d;
            }
        }
        int top = r.top + above;
        int bottom = top + row.endHeight();
        int contentEnd = this.content.getHeight() + all;
        int m = Ui.dp(this, 4.0f);
        int topRoom = m;
        if (this.firstInCard.get(index)) {
            // keep the section header (and the card's top padding) in view with its first row
            topRoom = Ui.dp(this, HEAD_DP + CARD_PAD_DP) + m;
        }
        int bottomRoom = m;
        int cur = this.scroll.endScrollY();
        int to = cur;
        if (index == 0) {
            to = 0;
        } else if (index == this.rows.size() - 1) {
            to = contentEnd - vh;
        } else if (top - topRoom < cur) {
            to = top - topRoom;
        } else if (bottom + bottomRoom > cur + vh) {
            to = bottom + bottomRoom - vh;
        }
        to = Math.max(0, Math.min(Math.max(0, contentEnd - vh), to));
        if (to != cur) {
            this.scroll.scrollToY(to, animate);
        }
    }

    // ------------------------------------------------------------------ actions

    private void flipIsrael() {
        boolean on = !this.prefs.israel();
        this.prefs.israel(on);
        // Follow the default city across the switch (Brooklyn <-> Jerusalem).
        if (on && "brooklyn".equals(this.prefs.city())) {
            this.prefs.city("jerusalem");
        } else if (!on && "jerusalem".equals(this.prefs.city())) {
            this.prefs.city("brooklyn");
        }
        this.israelToggle.setOn(on, true);
        // City value may have changed with it.
        SetRow city = row("city");
        if (city != null && city.value() instanceof TextView) {
            ((TextView) city.value()).setText(Cities.of(this.prefs.city()).name(this.lang));
        }
    }

    SetRow row(String key) {
        for (SetRow r : this.rows) {
            if (r.key.equals(key)) {
                return r;
            }
        }
        return null;
    }

    ChoicePopup choice() {
        return this.choice;
    }

    private ChoicePopup newChoice(String titleKey) {
        if (this.choice != null && this.choice.isShowing()) {
            this.choice.dismiss(null);
        }
        View bar = (View) this.barOk.getParent();
        int barPx = bar.getHeight() > 0 ? bar.getHeight() : Ui.dp(this, 21.0f);
        final ChoicePopup made = new ChoicePopup(this, this.top, this.pal, this.lang, Str.get(this.lang, titleKey), barPx);
        this.choice = made;
        this.barRight.setText(Str.get(this.lang, "cancel"));
        final int back = this.keep;
        made.onClose(new Runnable() {
            @Override
            public void run() {
                // A popup closing late (its 120 ms fade) must not undo a newer popup that is already open.
                if (isFinishing() || (choice != made && choice != null && choice.isShowing())) {
                    return;
                }
                barRight.setText(Str.get(lang, "back"));
                focusRow(back);
            }
        });
        return this.choice;
    }

    private void pickLayout() {
        ChoicePopup p = newChoice("layout");
        int sel = 0;
        for (int i = 0; i < Prefs.LAYOUTS.length; i++) {
            p.option(Str.get(this.lang, "l_" + Prefs.LAYOUTS[i]), null);
            if (Prefs.LAYOUTS[i].equals(this.prefs.layout())) {
                sel = i;
            }
        }
        p.onPick(new ChoicePopup.OnPick() {
            @Override
            public void pick(int i) {
                prefs.layout(Prefs.LAYOUTS[i]);
                build();
            }
        }).show(sel);
    }

    private void pickTheme() {
        ChoicePopup p = newChoice("theme");
        int sel = 0;
        for (int i = 0; i < Pal.ALL.length; i++) {
            Pal t = Pal.ALL[i];
            p.option(Str.get(this.lang, "t_" + t.id), t.swatches);
            if (t.id.equals(this.pal.id)) {
                sel = i;
            }
        }
        final int was = sel;
        p.onPick(new ChoicePopup.OnPick() {
            @Override
            public void pick(int i) {
                if (i != was) {
                    prefs.theme(Pal.ALL[i].id);
                    recreate(); // whole window re-themes; focus index survives via onSaveInstanceState
                }
            }
        }).show(sel);
    }

    private void pickSize() {
        ChoicePopup p = newChoice("fontSize");
        String[] keys = {"small", "medium", "large"};
        int sel = 1;
        for (int i = 0; i < Prefs.SIZES.length; i++) {
            p.option(Str.get(this.lang, keys[i]), null);
            if (Prefs.SIZES[i].equals(this.prefs.size())) {
                sel = i;
            }
        }
        p.onPick(new ChoicePopup.OnPick() {
            @Override
            public void pick(int i) {
                prefs.size(Prefs.SIZES[i]);
                build();
            }
        }).show(sel);
    }

    private void pickNusach() {
        ChoicePopup p = newChoice("nusach");
        for (int i = 0; i < Prefs.NUSACHOS.length; i++) {
            p.option(nusachName(Prefs.NUSACHOS[i]), null);
        }
        p.onPick(new ChoicePopup.OnPick() {
            @Override
            public void pick(int i) {
                prefs.nusach(Prefs.NUSACHOS[i]);
                build();
            }
        }).show(Names.nusachIndex(this.prefs.nusach()));
    }

    private void pickCity() {
        ChoicePopup p = newChoice("city");
        int sel = 0;
        for (int i = 0; i < Cities.ALL.length; i++) {
            p.option(Cities.ALL[i].name(this.lang), null);
            if (Cities.ALL[i].id.equals(this.prefs.city())) {
                sel = i;
            }
        }
        p.onPick(new ChoicePopup.OnPick() {
            @Override
            public void pick(int i) {
                prefs.city(Cities.ALL[i].id);
                build();
            }
        }).show(sel);
    }

    private void pickLang() {
        ChoicePopup p = newChoice("language");
        int sel = 0;
        for (int i = 0; i < Prefs.LANGS.length; i++) {
            p.option(langName(Prefs.LANGS[i]), null);
            if (Prefs.LANGS[i].equals(this.prefs.lang())) {
                sel = i;
            }
        }
        final int was = sel;
        p.onPick(new ChoicePopup.OnPick() {
            @Override
            public void pick(int i) {
                if (i != was) {
                    prefs.lang(Prefs.LANGS[i]);
                    recreate();
                }
            }
        }).show(sel);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (this.choice != null && this.choice.isShowing()) {
            if (this.choice.dispatch(e)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        if (this.choice != null && this.choice.isShowing()) {
            this.choice.dismiss(null);
            return;
        }
        super.onBackPressed();
    }

    // ------------------------------------------------------------------ labels

    private String nusachName(String id) {
        int i = Names.nusachIndex(id);
        return Str.rtl(this.lang) ? Names.nusachHe(id) : Names.NUSACH_EN[i];
    }

    static String langName(String str) {
        return "he".equals(str) ? "עברית" : "English";
    }

    static String sizeKey(String str) {
        return "s".equals(str) ? "small" : "l".equals(str) ? "large" : "medium";
    }
}
