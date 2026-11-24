package com.pureflip.siddur;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class Prefs {
    private final SharedPreferences sp;
    static final String[] NUSACHOS = {"ashkenaz", "sefard", "edot"};
    /** UI languages. Yiddish ("yi") was dropped; a saved "yi" is migrated to "he" (see {@link #lang()}). */
    static final String[] LANGS = {"en", "he"};
    static final String[] LAYOUTS = {"carousel", "split", "toc", "list"};
    static final String[] SIZES = {"s", "m", "l"};

    Prefs(Context context) {
        Context app = context.getApplicationContext();
        this.sp = (app != null ? app : context).getSharedPreferences("siddur", 0);
    }

    // Safe reads: a value of the wrong type (old build, restored backup) gives the default instead of a crash.
    private String str(String key, String def) {
        try {
            String v = this.sp.getString(key, def);
            return v != null ? v : def;
        } catch (ClassCastException e) {
            return def;
        }
    }

    private boolean bool(String key, boolean def) {
        try {
            return this.sp.getBoolean(key, def);
        } catch (ClassCastException e) {
            return def;
        }
    }

    private int num(String key, int def) {
        try {
            return this.sp.getInt(key, def);
        } catch (ClassCastException e) {
            return def;
        }
    }

    private static String oneOf(String v, String[] allowed, String def) {
        for (String a : allowed) {
            if (a.equals(v)) {
                return v;
            }
        }
        return def;
    }

    boolean setupDone() {
        return bool("setupDone", false);
    }

    void setSetupDone() {
        this.sp.edit().putBoolean("setupDone", true).apply();
    }

    String nusach() {
        return oneOf(str("nusach", "ashkenaz"), NUSACHOS, "ashkenaz");
    }

    void nusach(String str) {
        this.sp.edit().putString("nusach", str).apply();
    }

    String lang() {
        String l = str("lang", "en");
        if ("en".equals(l) || "he".equals(l)) {
            return l;
        }
        // Yiddish -> Hebrew (closest remaining UI language); anything else unknown -> English.
        String to = "yi".equals(l) ? "he" : "en";
        this.sp.edit().putString("lang", to).apply();
        return to;
    }

    void lang(String str) {
        this.sp.edit().putString("lang", str).apply();
        bump();
    }

    String layout() {
        return oneOf(str("layout", "list"), LAYOUTS, "list");
    }

    void layout(String str) {
        this.sp.edit().putString("layout", str).apply();
    }

    String theme() {
        String saved = str("theme", "forest");
        String id = migrateTheme(saved);
        if (!id.equals(saved)) {
            this.sp.edit().putString("theme", id).apply();
        }
        return id;
    }

    /**
     * Maps a saved theme id to one of the current ten. v0.1.0 had "forest", "cream" and "amber"; their
     * polished versions keep the same ids. Anything unknown falls back to Forest.
     */
    static String migrateTheme(String saved) {
        if (saved == null) {
            return "forest";
        }
        switch (saved) {
            case "cream_sage":
            case "creamsage":
                return "cream";
            case "amber_dark":
            case "dark":
                return "amber";
            case "light":
                return "ar_light";
            default:
                break;
        }
        for (Pal p : Pal.ALL) {
            if (p.id.equals(saved)) {
                return saved;
            }
        }
        return "forest";
    }

    void theme(String str) {
        this.sp.edit().putString("theme", str).apply();
        bump();
    }

    String size() {
        return oneOf(str("size", "m"), SIZES, "m");
    }

    void size(String str) {
        this.sp.edit().putString("size", str).apply();
    }

    String city() {
        return str("city", "brooklyn");
    }

    void city(String str) {
        this.sp.edit().putString("city", str).apply();
    }

    boolean israel() {
        return bool("israel", false);
    }

    void israel(boolean z) {
        this.sp.edit().putBoolean("israel", z).apply();
    }

    int styleGen() {
        return num("gen", 0);
    }

    private void bump() {
        this.sp.edit().putInt("gen", styleGen() + 1).apply();
    }

    float textSp() {
        String size = size();
        if ("s".equals(size)) {
            return 16.0f;
        }
        return "l".equals(size) ? 23.0f : 19.0f;
    }

    static float textSpFor(String str) {
        if ("s".equals(str)) {
            return 16.0f;
        }
        return "l".equals(str) ? 23.0f : 19.0f;
    }

    static final class Bookmark {
        String doc;
        int from;
        String nusach;
        int para;
        String title;
        int to;

        Bookmark() {
        }

        String enc() {
            return this.nusach + "|" + this.doc + "|" + this.para + "|" + this.from + "|" + this.to + "|" + this.title.replace("|", " ").replace("\n", " ");
        }

        static Bookmark dec(String str) {
            String[] split = str.split("\\|", 6);
            if (split.length < 6) {
                return null;
            }
            try {
                Bookmark bookmark = new Bookmark();
                bookmark.nusach = split[0];
                bookmark.doc = split[1];
                bookmark.para = Integer.parseInt(split[2]);
                bookmark.from = Integer.parseInt(split[3]);
                bookmark.to = Integer.parseInt(split[4]);
                bookmark.title = split[5];
                return bookmark;
            } catch (NumberFormatException unused) {
                return null;
            }
        }
    }

    List<Bookmark> bookmarks() {
        List<Bookmark> out = new ArrayList<>();
        for (String line : str("bookmarks", "").split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            Bookmark b = Bookmark.dec(line);
            if (b != null) {
                out.add(b);
            }
        }
        return out;
    }

    void saveBookmarks(List<Bookmark> list) {
        StringBuilder sb = new StringBuilder();
        Iterator<Bookmark> it = list.iterator();
        while (it.hasNext()) {
            sb.append(it.next().enc());
            sb.append('\n');
        }
        this.sp.edit().putString("bookmarks", sb.toString()).apply();
    }

    void addBookmark(Bookmark bookmark) {
        List<Bookmark> bookmarks = bookmarks();
        for (Bookmark bookmark2 : bookmarks) {
            if (bookmark2.nusach.equals(bookmark.nusach) && bookmark2.doc.equals(bookmark.doc) && bookmark2.para == bookmark.para && bookmark2.from == bookmark.from) {
                return;
            }
        }
        bookmarks.add(0, bookmark);
        while (bookmarks.size() > 50) {
            bookmarks.remove(bookmarks.size() - 1);
        }
        saveBookmarks(bookmarks);
    }
}
