package com.pureflip.siddur;

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.util.GeoLocation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class Luach {
    static final int MAARIV = 2;
    static final int MINCHA = 1;
    static final int SHACHARIS = 0;
    final Cities.City city;
    final boolean israel;
    final ZonedDateTime now;

    Luach(Cities.City city, boolean z, ZonedDateTime zonedDateTime) {
        this.city = city;
        this.israel = z;
        this.now = zonedDateTime;
    }

    static Luach current(Prefs prefs) {
        ZoneId systemDefault;
        Cities.City of = Cities.of(prefs.city());
        try {
            systemDefault = ZoneId.of(of.tz);
        } catch (Exception unused) {
            systemDefault = ZoneId.systemDefault();
        }
        return new Luach(of, prefs.israel(), ZonedDateTime.now(systemDefault));
    }

    private ComprehensiveZmanimCalendar zc(LocalDate localDate) {
        ComprehensiveZmanimCalendar comprehensiveZmanimCalendar = new ComprehensiveZmanimCalendar(new GeoLocation(this.city.en, this.city.lat, this.city.lon, this.city.elev, ZoneId.of(this.city.tz)));
        comprehensiveZmanimCalendar.setLocalDate(localDate);
        return comprehensiveZmanimCalendar;
    }

    private static boolean before(ZonedDateTime zonedDateTime, Instant instant) {
        return instant != null && zonedDateTime.toInstant().isBefore(instant);
    }

    int nowTefillah() {
        ComprehensiveZmanimCalendar zc = zc(this.now.toLocalDate());
        Instant alos72Minutes = zc.getAlos72Minutes();
        Instant chatzosHayom = zc.getChatzosHayom();
        Instant sunset = zc.getSunset();
        if (alos72Minutes != null && chatzosHayom != null && sunset != null) {
            if (before(this.now, alos72Minutes)) {
                return 2;
            }
            if (before(this.now, chatzosHayom)) {
                return 0;
            }
            return before(this.now, sunset) ? 1 : 2;
        }
        int hour = this.now.getHour();
        if (hour < 4 || hour >= 12) {
            return hour < 18 ? 1 : 2;
        }
        return 0;
    }

    JewishCalendar hebrewToday() {
        LocalDate localDate = this.now.toLocalDate();
        Instant tzaisGeonim8Point5Degrees = zc(localDate).getTzaisGeonim8Point5Degrees();
        if (tzaisGeonim8Point5Degrees != null && !before(this.now, tzaisGeonim8Point5Degrees)) {
            localDate = localDate.plusDays(1L);
        }
        return jc(localDate);
    }

    JewishCalendar jc(LocalDate localDate) {
        JewishCalendar jewishCalendar = new JewishCalendar(localDate);
        jewishCalendar.setInIsrael(this.israel);
        return jewishCalendar;
    }

    JewishCalendar dayFor(int i) {
        LocalDate localDate = this.now.toLocalDate();
        if (i == 2 && this.now.getHour() >= 12) {
            localDate = localDate.plusDays(1L);
        }
        if (i == -1) {
            return hebrewToday();
        }
        return jc(localDate);
    }

    String hebrewDate() {
        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        hebrewDateFormatter.setHebrewFormat(true);
        hebrewDateFormatter.setUseGershGershayim(true);
        return hebrewDateFormatter.format(hebrewToday());
    }

    int hebrewDayOfMonth() {
        return hebrewToday().getJewishDayOfMonth();
    }

    int hebrewDow() {
        return hebrewToday().getDayOfWeek() - 1;
    }

    Set<String> active(String str) {
        int i = "shacharis".equals(str) ? 0 : "mincha".equals(str) ? 1 : "maariv".equals(str) ? 2 : -1;
        JewishCalendar dayFor = dayFor(i);
        TefilaRules tefilaRules = new TefilaRules();
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add(tefilaRules.isMashivHaruachRecited(dayFor) ? "winter" : "summer");
        hashSet.add(tefilaRules.isVeseinTalUmatarRecited(dayFor) ? "talumatar" : "barcha");
        if (dayFor.isRoshChodesh()) {
            hashSet.add("rc");
        }
        if (dayFor.isCholHamoed()) {
            hashSet.add("ch");
        }
        if (dayFor.isPesach() || dayFor.isCholHamoedPesach()) {
            hashSet.add("pes");
        }
        if (dayFor.isSuccos() || dayFor.isCholHamoedSuccos()) {
            hashSet.add("suk");
        }
        if (dayFor.isShavuos()) {
            hashSet.add("shav");
        }
        if (dayFor.isShminiAtzeres() || dayFor.isSimchasTorah()) {
            hashSet.add("sa");
        }
        if (dayFor.isRoshHashana()) {
            hashSet.add("rh");
        }
        if (dayFor.isYomTovAssurBemelacha()) {
            hashSet.add("yt");
        }
        if (dayFor.isChanukah()) {
            hashSet.add("chan");
        }
        if (dayFor.isPurim()) {
            hashSet.add("pur");
        }
        if (dayFor.isAseresYemeiTeshuva()) {
            hashSet.add("ayt");
        }
        if ((dayFor.isTaanis() && !dayFor.isTishaBav()) || (dayFor.isTishaBav() && i == 1)) {
            hashSet.add("fast");
        }
        if (dayFor.isTishaBav()) {
            hashSet.add("9av");
        }
        int dayOfWeek = dayFor.getDayOfWeek();
        if (dayOfWeek == 7) {
            hashSet.add("shab");
        }
        if (dayOfWeek >= 1 && dayOfWeek <= 6) {
            hashSet.add("dow" + dayOfWeek);
        }
        boolean isTachanunRecitedMincha = i == 1 ? tefilaRules.isTachanunRecitedMincha(dayFor) : tefilaRules.isTachanunRecitedShacharis(dayFor);
        hashSet.add(isTachanunRecitedMincha ? "tach" : "notach");
        if (isTachanunRecitedMincha && (dayOfWeek == 2 || dayOfWeek == 5)) {
            hashSet.add("monthu");
        }
        if (dayOfWeek == 2 || dayOfWeek == 5 || dayFor.isRoshChodesh() || dayFor.isCholHamoed() || dayFor.isChanukah() || dayFor.isPurim() || dayFor.isTaanis()) {
            hashSet.add("torah");
        }
        if (dayFor.getDayOfOmer() > 0) {
            hashSet.add("omer");
        }
        if (i == 2 && dayOfWeek == 1) {
            hashSet.add("motsh");
        }
        if (tefilaRules.isMizmorLesodaRecited(dayFor)) {
            hashSet.add("letoda");
        }
        int jewishMonth = dayFor.getJewishMonth();
        int jewishDayOfMonth = dayFor.getJewishDayOfMonth();
        if (jewishMonth == 6 || (jewishMonth == 7 && jewishDayOfMonth <= 21)) {
            hashSet.add("ledavid");
        }
        return hashSet;
    }

    String insertions(int i) {
        Set<String> active = active(i == 0 ? "shacharis" : i == 1 ? "mincha" : "maariv");
        JewishCalendar dayFor = dayFor(i);
        ArrayList arrayList = new ArrayList<>();
        arrayList.add(active.contains("winter") ? "משיב הרוח" : "מוריד הטל");
        if (active.contains("talumatar")) {
            arrayList.add("טל ומטר");
        }
        if (active.contains("rc") || active.contains("ch")) {
            arrayList.add("יעלה ויבא");
        }
        if (active.contains("chan") || active.contains("pur")) {
            arrayList.add("על הנסים");
        }
        if (active.contains("ayt")) {
            arrayList.add("עשרת ימי תשובה");
        }
        if (active.contains("fast")) {
            arrayList.add("עננו");
        }
        if (i != 2) {
            arrayList.add(active.contains("tach") ? "תחנון" : "אין תחנון");
        }
        if (i == 2 && dayFor.getDayOfOmer() > 0) {
            arrayList.add("ספירת העומר " + Ui.gem(dayFor.getDayOfOmer()));
        }
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            if (i2 > 0) {
                sb.append(" · ");
            }
            sb.append((String) arrayList.get(i2));
        }
        return sb.toString();
    }
}
