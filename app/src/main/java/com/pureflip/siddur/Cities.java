package com.pureflip.siddur;

final class Cities {
    static final City[] ALL = {new City("brooklyn", "Brooklyn", "ברוקלין", 40.6337d, -73.9896d, 10.0d, "America/New_York", false), new City("williamsburg", "Williamsburg", "וויליאמסבורג", 40.7081d, -73.9571d, 10.0d, "America/New_York", false), new City("manhattan", "Manhattan", "מנהטן", 40.7831d, -73.9712d, 10.0d, "America/New_York", false), new City("queens", "Queens", "קווינס", 40.7282d, -73.7949d, 15.0d, "America/New_York", false), new City("staten", "Staten Island", "סטטן איילנד", 40.5795d, -74.1502d, 20.0d, "America/New_York", false), new City("five_towns", "Five Towns", "פייב טאונס", 40.6295d, -73.7271d, 5.0d, "America/New_York", false), new City("monsey", "Monsey", "מונסי", 41.1112d, -74.0685d, 150.0d, "America/New_York", false), new City("kj", "Kiryas Joel", "קרית יואל", 41.342d, -74.1677d, 170.0d, "America/New_York", false), new City("monroe", "Monroe", "מונרו", 41.3306d, -74.1868d, 200.0d, "America/New_York", false), new City("new_square", "New Square", "שיכון סקווירא", 41.1395d, -74.0304d, 130.0d, "America/New_York", false), new City("lakewood", "Lakewood", "לייקווד", 40.0821d, -74.2097d, 20.0d, "America/New_York", false), new City("passaic", "Passaic", "פסאיק", 40.8568d, -74.1285d, 30.0d, "America/New_York", false), new City("baltimore", "Baltimore", "בולטימור", 39.3643d, -76.7095d, 150.0d, "America/New_York", false), new City("philadelphia", "Philadelphia", "פילדלפיה", 39.9526d, -75.1652d, 20.0d, "America/New_York", false), new City("miami", "Miami Beach", "מיאמי ביץ'", 25.7907d, -80.13d, 2.0d, "America/New_York", false), new City("cleveland", "Cleveland", "קליבלנד", 41.4993d, -81.6944d, 200.0d, "America/New_York", false), new City("detroit", "Detroit", "דטרויט", 42.3314d, -83.0458d, 190.0d, "America/Detroit", false), new City("chicago", "Chicago", "שיקגו", 42.0111d, -87.7d, 180.0d, "America/Chicago", false), new City("los_angeles", "Los Angeles", "לוס אנג'לס", 34.0736d, -118.351d, 70.0d, "America/Los_Angeles", false), new City("toronto", "Toronto", "טורונטו", 43.728d, -79.431d, 170.0d, "America/Toronto", false), new City("montreal", "Montreal", "מונטריאול", 45.52d, -73.62d, 50.0d, "America/Toronto", false), new City("london", "London", "לונדון", 51.5718d, -0.075d, 30.0d, "Europe/London", false), new City("manchester", "Manchester", "מנצ'סטר", 53.51d, -2.26d, 50.0d, "Europe/London", false), new City("antwerp", "Antwerp", "אנטוורפן", 51.21d, 4.42d, 10.0d, "Europe/Brussels", false), new City("paris", "Paris", "פריז", 48.8566d, 2.3522d, 35.0d, "Europe/Paris", false), new City("zurich", "Zurich", "ציריך", 47.3769d, 8.5417d, 410.0d, "Europe/Zurich", false), new City("jerusalem", "Jerusalem", "ירושלים", 31.778d, 35.2354d, 780.0d, "Asia/Jerusalem", true), new City("bnei_brak", "Bnei Brak", "בני ברק", 32.0807d, 34.8338d, 30.0d, "Asia/Jerusalem", true), new City("beit_shemesh", "Beit Shemesh", "בית שמש", 31.747d, 34.9881d, 300.0d, "Asia/Jerusalem", true), new City("modiin_illit", "Modiin Illit", "מודיעין עילית", 31.933d, 35.041d, 300.0d, "Asia/Jerusalem", true), new City("ashdod", "Ashdod", "אשדוד", 31.8044d, 34.6553d, 20.0d, "Asia/Jerusalem", true), new City("tzfat", "Tzfas", "צפת", 32.9646d, 35.496d, 850.0d, "Asia/Jerusalem", true), new City("haifa", "Haifa", "חיפה", 32.794d, 34.9896d, 100.0d, "Asia/Jerusalem", true)};

    private Cities() {
    }

    static final class City {
        final double elev;
        final String en;
        final String he;
        final String id;
        final boolean israel;
        final double lat;
        final double lon;
        final String tz;

        City(String str, String str2, String str3, double d, double d2, double d3, String str5, boolean z) {
            this.id = str;
            this.en = str2;
            this.he = str3;
            this.lat = d;
            this.lon = d2;
            this.elev = d3;
            this.tz = str5;
            this.israel = z;
        }

        String name(String str) {
            return "en".equals(str) ? this.en : this.he;
        }
    }

    static City of(String str) {
        for (City city : ALL) {
            if (city.id.equals(str)) {
                return city;
            }
        }
        return ALL[0];
    }
}
