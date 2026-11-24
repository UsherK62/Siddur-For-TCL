package com.pureflip.siddur;

final class Names {
    static final String[] HOME_EN = {"Shacharis", "Mincha", "Maariv", "Birkas Hamazon", "Brachos", "Tehillim", "Bookmarks"};
    static final String[] HOME_HE = {"שחרית", "מנחה", "מעריב", "ברכת המזון", "ברכות", "תהילים", "סימניות"};
    static final String[] HOME_DOC = {"shacharis", "mincha", "maariv", "bentching", null, null, null};
    static final String[] NUSACH_EN = {"Ashkenaz", "Sefard", "Edot HaMizrach"};
    static final String[] NUSACH_HE = {"אשכנז", "ספרד", "עדות המזרח"};
    static final String[] BRACHOS_DOC = {"b_netilas", "b_food", "b_achrona", "b_sights", "b_derech", "b_hamita"};
    static final String[] BRACHOS_HE = {"נטילת ידים ואשר יצר", "ברכות הנהנין", "ברכה אחרונה", "ברכות הראייה", "תפילת הדרך", "קריאת שמע על המטה"};
    static final int[] MONTH_START = {1, 10, 18, 23, 29, 35, 39, 44, 49, 55, 60, 66, 69, 72, 77, 79, 83, 88, 90, 97, 104, 106, 108, 113, 119, 119, 120, 135, 140, 145};
    static final int[] MONTH_END = {9, 17, 22, 28, 34, 38, 43, 48, 54, 59, 65, 68, 71, 76, 78, 82, 87, 89, 96, 103, 105, 107, 112, 118, 119, 119, 134, 139, 144, 150};
    static final int[] WEEK_START = {1, 30, 51, 73, 90, 107, 120};
    static final int[] WEEK_END = {29, 50, 72, 89, 106, 119, 150};

    private Names() {
    }

    static int nusachIndex(String nusach) {
        for (int i = 0; i < Prefs.NUSACHOS.length; i++) {
            if (Prefs.NUSACHOS[i].equals(nusach)) {
                return i;
            }
        }
        return 0;
    }

    static String nusachHe(String nusach) {
        return NUSACH_HE[nusachIndex(nusach)];
    }

    static String docHe(String doc) {
        for (int i = 0; i < HOME_DOC.length; i++) {
            if (doc.equals(HOME_DOC[i])) {
                return HOME_HE[i];
            }
        }
        for (int i = 0; i < BRACHOS_DOC.length; i++) {
            if (doc.equals(BRACHOS_DOC[i])) {
                return BRACHOS_HE[i];
            }
        }
        return "tehillim".equals(doc) ? "תהילים" : doc;
    }

    static String docEn(String doc) {
        for (int i = 0; i < HOME_DOC.length; i++) {
            if (doc.equals(HOME_DOC[i])) {
                return HOME_EN[i];
            }
        }
        if ("tehillim".equals(doc)) {
            return "Tehillim";
        }
        return Str.get("en", doc);
    }
}
