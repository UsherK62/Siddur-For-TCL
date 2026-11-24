package com.pureflip.siddur;

/**
 * Colour themes. Ten palettes (from the approved theme mockups):
 * the three polished Siddur themes, Auto Reply's exact Light / Dark, and five more.
 *
 * <p>The old field names (ink, ink2, fill, pill, bar, ...) are kept so every screen keeps working; the
 * Auto Reply-style fields (acc2, focus0/1, head, off, pop, scrim, shadow) are new.
 */
final class Pal {
    static final Pal[] ALL;
    static final Pal FOREST;
    /**
     * Picker order, two groups, each running neutral -> green -> blue -> purple -> warm.
     * Light: White, Cream, Green, Sky blue, Purple, Peach. Dark: Black, Dark green, Black and gold, Brown.
     */
    static final String[] LIGHT = {"ar_light", "cream", "forest", "sky", "lavender", "peach"};
    static final String[] DARK = {"ar_dark", "teal", "amber", "coffee"};

    final String id;
    final boolean dark;
    final int bg;
    final int card;
    /** Main text. */
    final int ink;
    /** Secondary text (values, notes). */
    final int ink2;
    /** Dividers / hairlines. */
    final int line;
    final int acc;
    /** Second accent stop: OK chip, toggles and radio marks run acc -> acc2. */
    final int acc2;
    /** Focus fill gradient (start, end). {@link #fill} == focus0 for the older single-colour screens. */
    final int focus0;
    final int focus1;
    final int fill;
    final int ring;
    final int headerBg;
    final int headerFg;
    /** Hairline under the title strip. */
    final int barLine;
    /** Soft-key bar (always dark, white-ish labels, Auto Reply style). */
    final int bar;
    final int barFg;
    final int pill;
    final int pillFg;
    final int status;
    /** Toggle track when off; radio ring when not chosen. */
    final int off;
    /** Popup card. */
    final int pop;
    /** Popup scrim, ARGB. */
    final int scrim;
    /** Card shadow, ARGB. */
    final int shadow;
    /** Section header text. */
    final int head;
    final int info;
    final float radius;
    final boolean lightBars;
    final int[] swatches;

    private Pal(String id, boolean dark, int bg, int card, int ink, int ink2, int line, int acc,
                int acc2, int accFg, int focus0, int focus1, int ring, int headerBg, int headerFg,
                int barLine, int bar, int barFg, int status, int off, int pop, int scrim, int shadow, int head,
                int[] swatches) {
        this.id = id;
        this.dark = dark;
        this.bg = bg;
        this.card = card;
        this.ink = ink;
        this.ink2 = ink2;
        this.line = line;
        this.acc = acc;
        this.acc2 = acc2;
        this.focus0 = focus0;
        this.focus1 = focus1;
        this.fill = focus0;
        this.ring = ring;
        this.headerBg = headerBg;
        this.headerFg = headerFg;
        this.barLine = barLine;
        this.bar = bar;
        this.barFg = barFg;
        this.pill = acc;
        this.pillFg = accFg;
        this.status = status;
        this.off = off;
        this.pop = pop;
        this.scrim = scrim;
        this.shadow = shadow;
        this.head = head;
        this.info = mix(card, acc, dark ? 0.14f : 0.08f);
        this.radius = 11.0f;
        this.lightBars = !dark;
        this.swatches = swatches;
    }

    static {
        ALL = new Pal[]{
        new Pal("forest", false, 0xFFF3F7F4, 0xFFFFFFFF, 0xFF0B2216, 0xFF2C4636, 0xFFE3ECE6, 0xFF0A5A35,
                0xFF1F7A4C, 0xFFFFFFFF, 0xFFE2F2E8, 0xFFEDF6E4, 0xFF0A5A35, 0xFFFFFFFF, 0xFF08502F,
                0xFFD3E1D7, 0xFF0B2A1B, 0xFFFFFFFF, 0xFF053520, 0xFFB9C9BE, 0xFFFFFFFF, 0x7306150D, 0x100B2216, 0xFF0A5A35,
                new int[]{0xFFFFFFFF, 0xFF0A5A35, 0xFF0B2216}),
        new Pal("cream", false, 0xFFF5F0DE, 0xFFFFFCF0, 0xFF1C2A1A, 0xFF46553F, 0xFFE8E0C6, 0xFF2B6638,
                0xFF4A7F3A, 0xFFFFFFFF, 0xFFE5EED3, 0xFFEEF0D7, 0xFF2B6638, 0xFFEBE4C6, 0xFF1F4A29,
                0xFFC9BD93, 0xFF1C2A1A, 0xFFFFFFFF, 0xFF2A4A30, 0xFFCFC5A5, 0xFFFFFCF0, 0x6E1C2A1A, 0x123A3420, 0xFF2B6638,
                new int[]{0xFFF5F0DE, 0xFF2B6638, 0xFF1C2A1A}),
        new Pal("amber", true, 0xFF0F0D0A, 0xFF1B1813, 0xFFF3ECDD, 0xFFCDBD9E, 0xFF2E271C, 0xFFE8B655,
                0xFFD99A3A, 0xFF1A1408, 0xFF2E2414, 0xFF33260F, 0xFFE8B655, 0xFF1A1611, 0xFFF3ECDD,
                0xFF2E271C, 0xFF0A0907, 0xFFF3ECDD, 0xFF0A0907, 0xFF4A4134, 0xFF211C15, 0x96000000, 0x6E000000, 0xFFE8B655,
                new int[]{0xFF0F0D0A, 0xFFE8B655, 0xFFF3ECDD}),
        new Pal("ar_light", false, 0xFFF2F3F7, 0xFFFFFFFF, 0xFF12151C, 0xFF5D6575, 0xFFECEEF2, 0xFF4054D6,
                0xFF5B45CF, 0xFFFFFFFF, 0xFFEEF1FF, 0xFFF4F1FF, 0xFF4054D6, 0xFFFFFFFF, 0xFF12151C,
                0xFFE2E4E9, 0xFF171A21, 0xFFFFFFFF, 0xFF000000, 0xFFC3C8D2, 0xFFFFFFFF, 0x730E111A, 0x0F101828, 0xFF4054D6,
                new int[]{0xFFF2F3F7, 0xFF4054D6, 0xFF12151C}),
        new Pal("ar_dark", true, 0xFF0E1014, 0xFF1A1D24, 0xFFF1F3F7, 0xFFA3AAB8, 0xFF262A33, 0xFF8C9BFF,
                0xFFA58CFF, 0xFF0E1120, 0xFF242B47, 0xFF2A2648, 0xFF8C9BFF, 0xFF16191F, 0xFFF1F3F7,
                0xFF24272D, 0xFF0A0B0E, 0xFFFFFFFF, 0xFF000000, 0xFF454B57, 0xFF1D2129, 0x99000000, 0x6E000000, 0xFF8C9BFF,
                new int[]{0xFF0E1014, 0xFF8C9BFF, 0xFFF1F3F7}),
        new Pal("teal", true, 0xFF0A1916, 0xFF13261F, 0xFFEAF4F0, 0xFFA3BDB3, 0xFF20362F, 0xFF3CC8A0,
                0xFF2BAE8E, 0xFF04140F, 0xFF15392F, 0xFF173A35, 0xFF4FD1AA, 0xFF0F201B, 0xFFEAF4F0,
                0xFF1F352E, 0xFF06100D, 0xFFEAF4F0, 0xFF000000, 0xFF3A524B, 0xFF152A24, 0x96000000, 0x6E000000, 0xFF4FD1AA,
                new int[]{0xFF0A1916, 0xFF3CC8A0, 0xFFEAF4F0}),
        new Pal("sky", false, 0xFFEAF2F8, 0xFFFFFFFF, 0xFF102230, 0xFF4A6072, 0xFFE1EAF1, 0xFF1C78B4,
                0xFF2E93C9, 0xFFFFFFFF, 0xFFDDEEFA, 0xFFE6F4FB, 0xFF1C78B4, 0xFFFFFFFF, 0xFF145E8E,
                0xFFD2E0EA, 0xFF12293A, 0xFFFFFFFF, 0xFF12293A, 0xFFC3D1DC, 0xFFFFFFFF, 0x730B1A26, 0x1012293A, 0xFF1A6FA6,
                new int[]{0xFFEAF2F8, 0xFF2E93C9, 0xFF102230}),
        new Pal("coffee", true, 0xFF241A14, 0xFF33261D, 0xFFF5EBE0, 0xFFCDB8A3, 0xFF45352A, 0xFFE09A62,
                0xFFC97E48, 0xFF22150B, 0xFF4A3424, 0xFF4E3626, 0xFFE8A56E, 0xFF2C2018, 0xFFF5EBE0,
                0xFF3E2F25, 0xFF18110C, 0xFFF5EBE0, 0xFF100B08, 0xFF5C4A3C, 0xFF3A2B21, 0x96000000, 0x64000000, 0xFFE8A56E,
                new int[]{0xFF241A14, 0xFFE09A62, 0xFFF5EBE0}),
        new Pal("lavender", false, 0xFFEDE4F8, 0xFFFCF9FF, 0xFF1F1630, 0xFF5E5274, 0xFFECE6F5, 0xFF8240B0,
                0xFFA25CC9, 0xFFFFFFFF, 0xFFF0E1FA, 0xFFF6EAFB, 0xFF8240B0, 0xFFFCF9FF, 0xFF6E3398,
                0xFFE0D7EE, 0xFF24183A, 0xFFFFFFFF, 0xFF24183A, 0xFFD2C8E2, 0xFFFFFFFF, 0x731A1029, 0x1024183A, 0xFF7A3AA8,
                new int[]{0xFFEDE4F8, 0xFFA25CC9, 0xFF1F1630}),
        new Pal("peach", false, 0xFFFBEFE5, 0xFFFFF9F4, 0xFF2E1A10, 0xFF6E4E3C, 0xFFF2E2D5, 0xFFB8521C,
                0xFFD16A2E, 0xFFFFFFFF, 0xFFFBE1CF, 0xFFFCEAD9, 0xFFB8521C, 0xFFFFF9F4, 0xFF9A4415,
                0xFFEBD5C4, 0xFF3A2116, 0xFFFFF6EE, 0xFF3A2116, 0xFFE2CDBE, 0xFFFFF9F4, 0x6E2E1A10, 0x103A2116, 0xFFA84A18,
                new int[]{0xFFFBEFE5, 0xFFD16A2E, 0xFF2E1A10})
        };
        FOREST = ALL[0];
    }

    static Pal of(String id) {
        for (Pal pal : ALL) {
            if (pal.id.equals(id)) {
                return pal;
            }
        }
        return FOREST;
    }

    static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int alpha(int color, float a) {
        return (Math.round(a * 255.0f) << 24) | (color & 0xFFFFFF);
    }

    /** Linear blend of two opaque colours; t = 0 gives a, t = 1 gives b. */
    static int mix(int a, int b, float t) {
        float u = 1.0f - t;
        int r = Math.round(((a >> 16) & 255) * u + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * u + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * u + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
