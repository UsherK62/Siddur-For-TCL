package com.pureflip.siddur;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.Spanned;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.LruCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

final class Doc {
    private static final LruCache<String, Doc> CACHE = new LruCache<>(4);
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "siddur-text");
            thread.setPriority(4);
            return thread;
        }
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    final List<Item> items;
    final String key;
    final int sectionCount;

    interface Callback {
        void done(Doc doc);
    }

    static final class Item {
        final String[] cond;
        final boolean heading;
        final int level;
        final String plain;
        int section = -1;
        final CharSequence text;

        Item(boolean z, int i, String[] strArr, CharSequence charSequence, String str) {
            this.heading = z;
            this.level = i;
            this.cond = strArr;
            this.text = charSequence;
            this.plain = str;
        }

        boolean activeIn(Set<String> set) {
            String[] strArr = this.cond;
            if (strArr == null) {
                return true;
            }
            for (String str : strArr) {
                if (set.contains(str)) {
                    return true;
                }
            }
            return false;
        }
    }

    private Doc(String str, List<Item> list) {
        this.key = str;
        this.items = list;
        int i = -1;
        for (Item item : list) {
            if (item.heading && item.level == 1) {
                i++;
            }
            item.section = Math.max(i, 0);
        }
        this.sectionCount = Math.max(1, i + 1);
    }

    static final class FaceSpan extends MetricAffectingSpan {
        final Typeface tf;

        FaceSpan(Typeface typeface) {
            this.tf = typeface;
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            apply(textPaint);
        }

        @Override
        public void updateMeasureState(TextPaint textPaint) {
            apply(textPaint);
        }

        private void apply(Paint paint) {
            paint.setTypeface(this.tf);
            paint.setFakeBoldText(false);
        }
    }

    static CharSequence html(String str) {
        SpannableStringBuilder fast = fastHtml(str);
        if (fast != null) {
            return fast;
        }
        return slowHtml(str);
    }

    /** The original path: android.text.Html (slow on a weak phone: a TagSoup parser per paragraph). */
    static CharSequence slowHtml(String str) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(Html.fromHtml(str));
        for (StyleSpan styleSpan : (StyleSpan[]) spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), StyleSpan.class)) {
            if ((styleSpan.getStyle() & 1) != 0) {
                int spanStart = spannableStringBuilder.getSpanStart(styleSpan);
                int spanEnd = spannableStringBuilder.getSpanEnd(styleSpan);
                spannableStringBuilder.removeSpan(styleSpan);
                if (Ui.frankBold != null) {
                    spannableStringBuilder.setSpan(new FaceSpan(Ui.frankBold), spanStart, spanEnd, 33);
                }
            }
        }
        int length = spannableStringBuilder.length();
        while (length > 0 && Character.isWhitespace(spannableStringBuilder.charAt(length - 1))) {
            length--;
        }
        if (length < spannableStringBuilder.length()) {
            spannableStringBuilder.delete(length, spannableStringBuilder.length());
        }
        return spannableStringBuilder;
    }

    /**
     * Fast equivalent of {@link #slowHtml} for the only markup our texts use: &lt;b&gt;, &lt;small&gt;, &lt;big&gt;
     * and &lt;br&gt;, no entities. Same output as Html.fromHtml (whitespace collapsing, RelativeSizeSpan 0.8 / 1.25,
     * bold as {@link FaceSpan}, trailing whitespace trimmed); verified paragraph-by-paragraph in DocParseTest.
     * Returns null for anything else so the caller falls back to Html.
     */
    static SpannableStringBuilder fastHtml(String s) {
        if (s.indexOf('&') >= 0) {
            return null;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        // open-tag start offsets, nested (LIFO) like Html: kind 0 = b, 1 = small, 2 = big
        int[] openAt = new int[16];
        int[] openKind = new int[16];
        int depth = 0;
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '<') {
                int close = s.indexOf('>', i);
                if (close < 0) {
                    return null;
                }
                String tag = s.substring(i + 1, close);
                int kind;
                boolean end = tag.startsWith("/");
                String name = end ? tag.substring(1) : tag;
                switch (name) {
                    case "b":
                        kind = 0;
                        break;
                    case "small":
                        kind = 1;
                        break;
                    case "big":
                        kind = 2;
                        break;
                    case "br":
                    case "br/":
                    case "br /":
                        if (end) {
                            return null;
                        }
                        sb.append('\n');
                        i = close + 1;
                        continue;
                    default:
                        return null;
                }
                if (!end) {
                    if (depth == openAt.length) {
                        return null;
                    }
                    openAt[depth] = sb.length();
                    openKind[depth] = kind;
                    depth++;
                } else {
                    // Html closes the innermost open tag of the same kind
                    int k = depth - 1;
                    while (k >= 0 && openKind[k] != kind) {
                        k--;
                    }
                    if (k != depth - 1) {
                        return null; // mis-nested: leave it to Html
                    }
                    int start = openAt[k];
                    depth--;
                    if (start != sb.length()) {
                        Object span = kind == 0 ? (Ui.frankBold != null ? new FaceSpan(Ui.frankBold) : null)
                                : new RelativeSizeSpan(kind == 1 ? 0.8f : 1.25f);
                        if (span != null) {
                            sb.setSpan(span, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
                i = close + 1;
                continue;
            }
            if (c == '>') {
                return null;
            }
            if (c == ' ' || c == '\n') {
                // Html collapses runs of whitespace and drops it at the start / after a line break.
                int len = sb.length();
                char pred = len == 0 ? '\n' : sb.charAt(len - 1);
                if (pred != ' ' && pred != '\n') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        if (depth != 0) {
            return null;
        }
        int length = sb.length();
        while (length > 0 && Character.isWhitespace(sb.charAt(length - 1))) {
            length--;
        }
        if (length < sb.length()) {
            sb.delete(length, sb.length());
        }
        return sb;
    }

    static Doc parse(String str, BufferedReader bufferedReader) throws IOException {
        ArrayList arrayList = new ArrayList<>();
        while (true) {
            String readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (!readLine.isEmpty()) {
                    String[] split = readLine.split("\t", -1);
                    if (split[0].equals("H") && split.length >= 4) {
                        String[] split2 = split[2].isEmpty() ? null : split[2].split(",");
                        int parseInt = Integer.parseInt(split[1]);
                        String str2 = split[3];
                        arrayList.add(new Item(true, parseInt, split2, str2, str2));
                    } else if (split[0].equals("P") && split.length >= 3) {
                        arrayList.add(new Item(false, 0, split[1].isEmpty() ? null : split[1].split(","), html(split[2]), null));
                    }
                }
            } else {
                return new Doc(str, arrayList);
            }
        }
    }

    static String path(String str, String str2) {
        if ("tehillim".equals(str2)) {
            return "t/tehillim.txt";
        }
        return "t/" + str + "/" + str2 + ".txt";
    }

    static Doc cached(String nusach, String doc) {
        synchronized (CACHE) {
            return CACHE.get(path(nusach, doc));
        }
    }

    static Doc loadSync(Context context, String nusach, String doc) throws IOException {
        String path = path(nusach, doc);
        synchronized (CACHE) {
            Doc hit = CACHE.get(path);
            if (hit != null) {
                return hit;
            }
        }
        Ui.fonts(context);
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(context.getAssets().open(path), Charset.forName("UTF-8")), 16384)) {
            Doc d = parse(path, r);
            synchronized (CACHE) {
                CACHE.put(path, d);
            }
            return d;
        }
    }

    /** Loads on the text thread and calls back on the main thread (or synchronously when cached). */
    static void load(Context context, final String nusach, final String doc, final Callback cb) {
        Doc hit = cached(nusach, doc);
        if (hit != null) {
            cb.done(hit);
            return;
        }
        final Context app = context.getApplicationContext();
        IO.execute(new Runnable() {
            @Override
            public void run() {
                Doc d;
                try {
                    d = loadSync(app, nusach, doc);
                } catch (IOException | RuntimeException e) {
                    d = null; // missing / damaged asset: the screen gets null instead of the text thread dying
                }
                if (cb != null) {
                    final Doc fr = d;
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.done(fr);
                        }
                    });
                }
            }
        });
    }

    /** Runs background work on the (single) text thread. */
    static void runIo(Runnable r) {
        IO.execute(r);
    }

    static void preload(Context context, String str, String str2) {
        if (cached(str, str2) == null) {
            load(context, str, str2, null);
        }
    }
}
