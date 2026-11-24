package com.pureflip.siddur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import org.junit.Test;

/** Zmanim / luach smoke tests for Brooklyn (the app's default city). Plain JVM, no Android. */
public class LuachTest {
    private static final ZoneId NY = ZoneId.of("America/New_York");

    private static Luach brooklyn(int hour, int minute, boolean israel) {
        return new Luach(Cities.of("brooklyn"), israel, ZonedDateTime.of(2026, 11, 23, hour, minute, 0, 0, NY));
    }

    @Test
    public void brooklynIsTheDefaultCity() {
        Cities.City c = Cities.of("brooklyn");
        assertEquals("Brooklyn", c.en);
        assertEquals("America/New_York", c.tz);
        assertFalse(c.israel);
    }

    @Test
    public void currentTefillahFollowsTheDay() {
        // 23 Nov 2026 in Brooklyn: alos ~5:20, chatzos ~11:45, sunset ~16:30 (EST).
        assertEquals(Luach.MAARIV, brooklyn(4, 0, false).nowTefillah());
        assertEquals(Luach.SHACHARIS, brooklyn(8, 0, false).nowTefillah());
        assertEquals(Luach.SHACHARIS, brooklyn(11, 30, false).nowTefillah());
        assertEquals(Luach.MINCHA, brooklyn(12, 30, false).nowTefillah());
        assertEquals(Luach.MINCHA, brooklyn(16, 0, false).nowTefillah());
        assertEquals(Luach.MAARIV, brooklyn(17, 0, false).nowTefillah());
    }

    @Test
    public void hebrewDateRollsOverAfterTzais() {
        Luach morning = brooklyn(9, 0, false);
        Luach night = brooklyn(21, 0, false);
        assertEquals(morning.hebrewDayOfMonth() + 1, night.hebrewDayOfMonth());
        String d = morning.hebrewDate();
        System.out.println("Brooklyn 2026-11-23 09:00 -> " + d + " / day " + morning.hebrewDayOfMonth()
                + " / dow " + morning.hebrewDow() + " / mincha set " + morning.active("mincha"));
        assertTrue(d, d.contains("כסלו"));
        assertEquals(1, morning.hebrewDow()); // Monday (Sunday = 0)
    }

    @Test
    public void seasonalInsertionsDependOnIsrael() {
        Set<String> chul = brooklyn(9, 0, false).active("shacharis");
        assertTrue(chul.toString(), chul.contains("winter"));
        assertTrue("v'sein bracha until Dec 4 in chutz la'aretz: " + chul, chul.contains("barcha"));
        Set<String> eretz = brooklyn(9, 0, true).active("shacharis");
        assertTrue("tal umatar from 7 Cheshvan in Israel: " + eretz, eretz.contains("talumatar"));
    }
}
