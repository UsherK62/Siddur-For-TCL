package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** The fast paragraph parser gives exactly what android.text.Html gave, for every paragraph we ship. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class DocParseTest {
    private static String describe(CharSequence cs) {
        StringBuilder b = new StringBuilder();
        b.append(cs.toString()).append('|');
        if (cs instanceof Spanned) {
            Spanned sp = (Spanned) cs;
            List<String> spans = new ArrayList<>();
            for (Object o : sp.getSpans(0, sp.length(), Object.class)) {
                String kind = o.getClass().getSimpleName();
                if (o instanceof RelativeSizeSpan) {
                    kind += ((RelativeSizeSpan) o).getSizeChange();
                }
                if (o instanceof Doc.FaceSpan) {
                    kind += ((Doc.FaceSpan) o).tf == Ui.frankBold ? "bold" : "?";
                }
                spans.add(kind + "@" + sp.getSpanStart(o) + "-" + sp.getSpanEnd(o) + "/" + sp.getSpanFlags(o));
            }
            java.util.Collections.sort(spans);
            b.append(spans);
        }
        return b.toString();
    }

    @Test
    public void fastParserMatchesHtmlForEveryParagraph() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        Ui.fonts(ctx);
        List<String> files = new ArrayList<>();
        for (String n : Prefs.NUSACHOS) {
            for (String f : ctx.getAssets().list("t/" + n)) {
                files.add("t/" + n + "/" + f);
            }
        }
        files.add("t/tehillim.txt");
        int checked = 0;
        for (String path : files) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open(path), Charset.forName("UTF-8")))) {
                String line;
                int no = 0;
                while ((line = r.readLine()) != null) {
                    no++;
                    String[] p = line.split("\t", -1);
                    if (p.length >= 3 && p[0].equals("P")) {
                        CharSequence fast = Doc.fastHtml(p[2]);
                        assertNotNull(path + ":" + no, fast);
                        assertEquals(path + ":" + no, describe(Doc.slowHtml(p[2])), describe(fast));
                        checked++;
                    }
                }
            }
        }
        assertEquals(true, checked > 5000);
    }

    @Test
    public void unknownMarkupFallsBack() {
        assertNull(Doc.fastHtml("a &amp; b"));
        assertNull(Doc.fastHtml("<i>x</i>"));
        assertNull(Doc.fastHtml("<b>unclosed"));
        assertEquals("a & b", Doc.html("a &amp; b").toString());
    }
}
