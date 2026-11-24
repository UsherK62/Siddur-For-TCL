# Third-party licenses

This project includes or depends on the works below. The app's own code is GPL-2.0-only (GNU General Public License, version 2 only) with an exception for Apache-2.0 libraries; see `LICENSE`.

License compatibility with GPL-2.0-only:

- **KosherJava Zmanim (LGPL 2.1):** compatible. The library is used unmodified as a normal dependency, which LGPL 2.1 allows in a work under any license. Section 3 of LGPL 2.1 also allows its code to be used under GPL version 2. Full text: `licenses/LGPL-2.1-KosherJava.txt`.
- **desugar_jdk_libs (GPL 2.0 with the Classpath Exception):** a build-time Android library that backports `java.time` to old Android versions. It is linked into the APK under the Classpath Exception.
- **Fonts (SIL OFL 1.1):** each font is bundled as a separate font file in `app/src/main/assets/fonts/` and stays under the OFL. Inter and Heebo are subsets; under the OFL, a subset is a Modified Version and keeps the same license.
- **Texts:** data, not code, kept under their own terms with credit (below).

## KosherJava Zmanim 3.0.0 (GNU Lesser General Public License 2.1)

Zmanim and Jewish calendar calculations use the Gradle / Maven dependency `com.kosherjava:zmanim:3.0.0`. The library is not copied into this tree. Source and license: [KosherJava/zmanim](https://github.com/KosherJava/zmanim). License text: `licenses/LGPL-2.1-KosherJava.txt`.

## Fonts (SIL Open Font License 1.1)

| Font | Copyright | License file |
|---|---|---|
| Frank Ruhl Libre | Copyright 2015 The Frank Ruhl Libre Project Authors | `licenses/OFL-FrankRuhlLibre.txt` |
| IBM Plex Sans Hebrew | Copyright © 2017 IBM Corp. with Reserved Font Name "Plex" | `licenses/OFL-IBMPlexSansHebrew.txt` |
| Inter (subset) | Copyright The Inter Project Authors | `licenses/OFL-Inter.txt` (also in the APK) |
| Heebo (subset) | Copyright The Heebo Project Authors | `licenses/OFL-Heebo.txt` (also in the APK) |

## Texts

- Ashkenaz and Sefard weekday tefillos and Birkas Hamazon: The Metsudah Siddur, 1981 (Avrohom Davis), via Sefaria, [CC BY](https://creativecommons.org/licenses/by/3.0/). The text is converted to this app's own format and section layout.
- Ashkenaz brachos: Daat Siddur Ashkenaz (daat.ac.il), public domain.
- Sefard brachos: Torat Emet 357, public domain.
- Edot HaMizrach: Siddur Edot HaMizrach, Shaliehsaboo Edition, [CC0](https://creativecommons.org/publicdomain/zero/1.0/).
- Tehillim: Tanach with Nikkud (tanach.us), public domain.

The About screen in the app lists these credits too.
