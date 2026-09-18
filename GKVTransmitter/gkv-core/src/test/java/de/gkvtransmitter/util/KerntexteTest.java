package de.gkvtransmitter.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Haelt die Textsammlung des Kerns mit dem Quelltext zusammen.
 *
 * <p>Dasselbe Verfahren wie bei {@code BefundkatalogTest} und aus demselben
 * Grund: <b>Ein Schluessel ohne Eintrag faellt sonst erst auf dem Bildschirm
 * auf</b> - und dann als der Schluessel selbst, weil {@code AppMessages} ihn
 * zurueckgibt, wenn es nichts findet. Das ist im Entwickeln nuetzlich und im
 * Betrieb eine Zumutung.</p>
 */
@DisplayName("Die Textsammlung des Kerns")
class KerntexteTest {

    /** Wo der Kern Texte abruft. */
    private static final Path QUELLTEXT = Path.of("src", "main", "java");

    /** {@code Kerntexte.get("schluessel")} und {@code Kerntexte.format("schluessel", ...)}. */
    private static final Pattern ABRUF =
            Pattern.compile("Kerntexte\\.(?:get|format)\\(\\s*\"([a-z][a-zA-Z0-9_.]+)\"");

    private static Set<String> abgerufeneSchluessel() throws IOException {
        Set<String> schluessel = new TreeSet<>();
        try (Stream<Path> dateien = Files.walk(QUELLTEXT)) {
            for (Path datei : dateien.filter(pfad -> pfad.toString().endsWith(".java")).toList()) {
                Matcher treffer = ABRUF.matcher(Files.readString(datei, StandardCharsets.UTF_8));
                while (treffer.find()) {
                    schluessel.add(treffer.group(1));
                }
            }
        }
        return schluessel;
    }

    @Test
    @DisplayName("kennt jeden Schluessel, den der Kern abruft")
    void kenntJedenSchluessel() throws IOException {
        Set<String> fehlend = new TreeSet<>();
        for (String schluessel : abgerufeneSchluessel()) {
            // AppMessages gibt den Schluessel zurueck, wenn es nichts findet -
            // genau daran ist eine Luecke zu erkennen.
            if (schluessel.equals(Kerntexte.get(schluessel))) {
                fehlend.add(schluessel);
            }
        }

        assertTrue(fehlend.isEmpty(),
                "Diese Schluessel fehlen in messages/kern-texte.json: " + fehlend);
    }

    /**
     * Und die Gegenrichtung, ueber Stichproben.
     *
     * <p>Vollstaendig liesse sie sich nur pruefen, wenn die Datei selbst
     * gelesen wuerde; das taete derselben Sache zweimal genuege. Diese drei
     * stehen fuer die drei Gruppen - Wartung, Blatt, Wert - und fallen aus,
     * sobald eine davon umbenannt wird.</p>
     */
    @Test
    @DisplayName("liefert die hinterlegten Texte und nicht die Schluessel")
    void liefertTexte() {
        assertEquals("Zertifikat erneuern", Kerntexte.get("wartung.zertifikat"));
        assertEquals("Telefon", Kerntexte.get("blatt.feld.telefon"));
        assertEquals("50 (Leistungsbereich F)",
                Kerntexte.format("blatt.wert.leistungsbereich", "50", "F"));
    }

    /**
     * Umlaute sind hier erlaubt - und nur hier.
     *
     * <p>Java-Quelltextkommentare vermeiden sie im ganzen Projekt
     * ({@code Pruefung}), und die Textliterale hatten das mitgeerbt: Das
     * Registrierungsblatt trug „REGISTRIERUNGSBLATT FUER" und „Strasse".
     * <b>In einer Textdatei gilt die Regel nicht</b>, und der Anwender liest
     * den Text, nicht den Quelltext.</p>
     */
    @Test
    @DisplayName("darf Umlaute tragen, wo der Quelltext keine hat")
    void umlauteSindErlaubt() {
        assertTrue(Kerntexte.get("blatt.titel").contains("FÜR"), Kerntexte.get("blatt.titel"));
        assertTrue(Kerntexte.get("blatt.feld.strasse").contains("Straße"));
    }
}
