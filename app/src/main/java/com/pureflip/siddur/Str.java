package com.pureflip.siddur;

import java.util.HashMap;
import java.util.Map;

final class Str {
    static final String CREDITS = "Ashkenaz and Sefard weekday tefillos and Birkas Hamazon: The Metsudah Siddur, 1981 (Avrohom Davis), via Sefaria, CC BY.\nAshkenaz brachos: Daat Siddur Ashkenaz (daat.ac.il), Public Domain.\nSefard brachos: Torat Emet 357, Public Domain.\nEdot HaMizrach: Siddur Edot HaMizrach, Shaliehsaboo Edition, CC0.\nTehillim: Tanach with Nikkud (tanach.us), Public Domain.\nZmanim and calendar: KosherJava Zmanim 3.0.0, LGPL 2.1.\nFonts: Frank Ruhl Libre, IBM Plex Sans Hebrew, Inter and Heebo, SIL Open Font License 1.1.";
    private static final Map<String, String[]> M = new HashMap<>();
    static final String[] WEEK_EN;
    static final String[] WEEK_HE;

    private Str() {
    }

    static {
        p("app", "Siddur", "סידור");
        p("settings", "Settings", "הגדרות");
        p("layout", "Layout", "צורה");
        p("whichLayout", "Which layout?", "איזו צורה?");
        p("theme", "Theme", "צבעים");
        p("whichTheme", "Which theme?", "אילו צבעים?");
        p("fontSize", "Font size", "גודל אותיות");
        p("fontSizeSub", "Tefillah text size", "גודל אותיות התפילה");
        p("nusach", "Nusach", "נוסח");
        p("whichNusach", "Which nusach?", "איזה נוסח?");
        p("city", "City", "עיר");
        p("israel", "In Israel", "בארץ ישראל");
        p("language", "Language", "שפה");
        p("about", "About", "אודות");
        p("on", "On", "כן");
        p("off", "Off", "לא");
        p("small", "Small", "קטן");
        p("medium", "Medium", "בינוני");
        p("large", "Large", "גדול");
        p("l_carousel", "Carousel", "אחד בכל פעם");
        p("l_split", "Split", "שני צדדים");
        p("l_toc", "Table of contents", "תוכן העניינים");
        p("l_list", "List", "שורות");
        p("t_forest", "Forest", "ירוק");
        p("t_cream", "Cream Sage", "שמנת");
        p("t_amber", "Amber dark", "זהב כהה");
        p("t_ar_light", "Light", "בהיר");
        p("t_ar_dark", "Dark", "כהה");
        p("t_teal", "Deep teal", "ירוק כהה");
        p("t_sky", "Sky blue", "תכלת");
        p("t_coffee", "Coffee", "חום");
        p("t_lavender", "Lavender", "סגול");
        p("t_peach", "Peach", "אפרסק");
        p("menu", "Menu", "אפשרויות");
        p("ok", "OK", "OK");
        p("back", "Back", "חזרה");
        p("notSaid", "Not said today", "לא אומרים היום");
        p("sections", "Sections", "חלקי התפילה");
        p("addBookmark", "Add bookmark", "הוסף סימניה");
        p("bookmarkAdded", "Bookmark added", "נוספה סימניה");
        p("deleteBookmark", "Delete bookmark", "מחק סימניה");
        p("noBookmarks", "No bookmarks yet", "אין סימניות עדיין");
        p("toStart", "Back to the start", "לתחילת התפילה");
        p("todayTehillim", "Today's Tehillim", "תהילים של היום");
        p("byMonth", "By day of month", "לפי ימי החודש");
        p("byWeek", "By day of week", "לפי ימי השבוע");
        p("byPerek", "By kapitel", "לפי פרק");
        p("day", "Day", "יום");
        p("kapitel", "Kapitel", "פרק");
        p("delete", "Delete", "מחק");
        p("cancel", "Cancel", "ביטול");
        p("version", "Version", "גרסה");
        p("b_netilas", "Netilas Yadayim", "");
        p("b_food", "Brachos on food", "");
        p("b_achrona", "After-brachos", "");
        p("b_sights", "Sights and sounds", "");
        p("b_derech", "Tefillas Haderech", "");
        p("b_hamita", "Shema al Hamitah", "");
        WEEK_EN = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Shabbos"};
        WEEK_HE = new String[]{"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "שבת"};
    }

    /** key, English, Hebrew (an empty Hebrew falls back to English). */
    private static void p(String key, String en, String he) {
        M.put(key, new String[]{en, he});
    }

    /** 0 = English, 1 = Hebrew. (Yiddish was dropped as a UI language; Prefs migrates "yi" to "he".) */
    static int idx(String str) {
        return "he".equals(str) ? 1 : 0;
    }

    static String get(String str, String str2) {
        String[] strArr = M.get(str2);
        if (strArr == null) {
            return str2;
        }
        String str3 = strArr[idx(str)];
        return str3.isEmpty() ? strArr[0] : str3;
    }

    static boolean rtl(String str) {
        return !"en".equals(str);
    }

    static String week(String str, int i) {
        int idx = idx(str);
        return (idx == 1 ? WEEK_HE : WEEK_EN)[i];
    }

    static String aboutBody(String str) {
        return idx(str) == 1
                ? "סידור פיורפליפ. תפילות ימי החול, ברכת המזון, ברכות ותהילים, ללא אינטרנט.\n\nזוהי מהדורה דיגיטלית ואינה פסק הלכה. הנוסח עשוי להיות שונה מעט מהסידור המודפס שלכם."
                : "PureFlip Siddur. Weekday tefillos, Birkas Hamazon, brachos and Tehillim, fully offline.\n\nThis is a digital edition, not a posek. Wording may differ slightly from the printed siddur you use.";
    }
}
