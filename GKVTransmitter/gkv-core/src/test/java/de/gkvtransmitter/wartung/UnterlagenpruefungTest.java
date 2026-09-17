package de.gkvtransmitter.wartung;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Pruefung auf neue Fassungen.
 *
 * <p><b>Ohne Netz.</b> Der Seitenabruf ist eine einmethodige Schnittstelle,
 * damit die Auswertung pruefbar bleibt - dieselbe Ueberlegung wie bei
 * {@code Datenaustauschreferenzen} und {@code Protokollfuehrung}. Ein Test, der
 * eine Webseite braucht, prueft das Netz und nicht den Quelltext.</p>
 */
@DisplayName("Die Pruefung auf neue Unterlagen")
class UnterlagenpruefungTest {

    private static Unterlagenpruefung mitSeite(String inhalt) {
        return new Unterlagenpruefung(Unterlagenstand.lade(), adresse -> inhalt);
    }

    /**
     * Ein Ausschnitt der echten Seite - <b>mitsamt Archiv</b>.
     *
     * <p>Die Bezugsquelle fuehrt jede Fassung seit 2008. Die Namen weichen
     * dabei von unseren ab: Was hier
     * {@code Anhang_3_Kostentraegerdatei_V10_20260414.pdf} heisst, steht dort
     * als {@code Anhang_03_Anlage_1_TP5_V10_20260414.pdf}. Beides zusammen -
     * Archiv und abweichende Namen - hat die erste Fassung dieser Pruefung
     * achtzehn Treffer melden lassen, darunter eine Datei von 2012.</p>
     */
    private static final String UNVERAENDERT = """
            <a href="/media/Anlage_1_TP5_V7_20110610.pdf">Anlage 1, alt</a>
            <a href="/media/Anlage_1_TP5_V20_20240426.pdf">Anlage 1, alt</a>
            <a href="/media/Anlage_1_TP5_V21_20260115.pdf">Anlage 1</a>
            <a href="/media/Anlage_3_TP5_V21_20250919.pdf">Anlage 3, alt</a>
            <a href="/media/Anlage_3_TP5_V22_20260218.pdf">Anlage 3</a>
            <a href="/media/Anhang_1_Anlage_1_TP5_20170831.pdf">Anhang 1</a>
            <a href="/media/Anhang_03_Anlage_1_TP5_20120912.pdf">Anhang 3, alt</a>
            <a href="/media/Anhang_03_Anlage_1_TP5_V10_20260414.pdf">Anhang 3</a>
            <a href="/media/Anlage_5_TP5_V4_0_20180927.pdf">geht uns nichts an</a>
            """;

    /**
     * Archivstaende sind nichts Neues.
     *
     * <p>Das ist beim ersten Lauf gegen die echte Seite herausgekommen und in
     * keinem Test davor: Der Vergleich lief gegen "alles Hinterlegte" statt je
     * Unterlage und meldete deshalb vierzehn Jahre alte Dateien als neuer.</p>
     */
    @Test
    @DisplayName("meldet bei unveraendertem Stand nichts Neues, auch nicht aus dem Archiv")
    void nichtsNeues() {
        Unterlagenpruefung.Ergebnis ergebnis = mitSeite(UNVERAENDERT).pruefe();

        assertTrue(ergebnis.erreichbar());
        assertFalse(ergebnis.gibtNeueres(), "Gefunden: " + ergebnis.neuere());
        assertTrue(ergebnis.gefunden() > 5, "Die Dateien selbst muessen erkannt worden sein");
    }

    /**
     * Was uns nichts angeht, wird uebergangen.
     *
     * <p>Auf der Seite stehen fast hundert Dateien; die meisten gehen diese
     * Anwendung nichts an. Sie als "neu" zu melden, waere derselbe Fehler wie
     * das Archiv zu melden.</p>
     */
    @Test
    @DisplayName("uebergeht Unterlagen, die nicht hinterlegt sind")
    void fremdeUnterlagen() {
        Unterlagenpruefung.Ergebnis ergebnis =
                mitSeite("<a href=\"Anlage_5_TP5_V9_20990101.pdf\">fremd</a>").pruefe();

        assertTrue(ergebnis.erreichbar());
        assertEquals(1, ergebnis.gefunden(), "Erkannt schon");
        assertFalse(ergebnis.gibtNeueres(), "Aber nicht gemeldet: " + ergebnis.neuere());
    }

    /**
     * Verglichen wird ueber den Stand, nicht ueber die Versionsnummer.
     *
     * <p>Eine Fassung kann fortgeschrieben werden, ohne dass die Version
     * steigt - genau das ist zwischen {@code Anlage_3_TP5_V22_20260218} und
     * {@code ..._20260521} geschehen.</p>
     */
    @Test
    @DisplayName("erkennt eine neuere Fassung derselben Version")
    void neuererStandGleicheVersion() {
        Unterlagenpruefung.Ergebnis ergebnis =
                mitSeite(UNVERAENDERT + "<a href=\"Anlage_3_TP5_V22_20270105.pdf\">neu</a>").pruefe();

        assertTrue(ergebnis.gibtNeueres());
        assertEquals("Anlage_3_TP5_V22_20270105.pdf", ergebnis.neuere().get(0).dateiname());
    }

    @Test
    @DisplayName("erkennt eine neue Version")
    void neueVersion() {
        Unterlagenpruefung.Ergebnis ergebnis =
                mitSeite(UNVERAENDERT + "<a href=\"Anlage_1_TP5_V23_20280101.pdf\">neu</a>").pruefe();

        assertTrue(ergebnis.gibtNeueres());
        assertEquals("23", ergebnis.neuere().get(0).version());
    }

    /**
     * Ein Fehlschlag ist kein Fehler.
     *
     * <p>Kein Netz, Seite umgebaut, Zeitueberlauf: Das meldet sich als Hinweis
     * und haelt nichts auf. Sonst stuende die Abrechnung still, weil eine
     * Webseite sich geaendert hat - deshalb wirft die Pruefung nichts.</p>
     */
    @Test
    @DisplayName("wirft nichts, wenn die Seite nicht zu erreichen ist")
    void ohneNetz() {
        Unterlagenpruefung pruefung = new Unterlagenpruefung(Unterlagenstand.lade(), adresse -> {
            throw new java.net.ConnectException("Kein Netz");
        });

        Unterlagenpruefung.Ergebnis ergebnis = pruefung.pruefe();

        assertFalse(ergebnis.erreichbar());
        assertEquals("Kein Netz", ergebnis.hinweis());
        assertFalse(ergebnis.gibtNeueres());
    }

    /**
     * "Nichts erkannt" ist nicht "alles aktuell".
     *
     * <p>Wer die beiden gleich behandelt, meldet Aktualitaet, weil eine
     * Webseite umgebaut wurde. Der Unterschied entscheidet darueber, ob jemand
     * nachsieht.</p>
     */
    @Test
    @DisplayName("unterscheidet eine umgebaute Seite von einem aktuellen Stand")
    void umgebauteSeite() {
        Unterlagenpruefung.Ergebnis ergebnis = mitSeite("<html>Nichts, was nach PDF aussieht.</html>").pruefe();

        assertTrue(ergebnis.erreichbar());
        assertTrue(ergebnis.nichtsErkannt(), "Kein Dateiname passte auf das Muster");
        assertFalse(ergebnis.gibtNeueres());
    }

    /** Dieselbe Datei mehrfach verlinkt ergibt einen Treffer. */
    @Test
    @DisplayName("zaehlt eine mehrfach verlinkte Datei einmal")
    void keineDoppelten() {
        List<Unterlagenpruefung.Gefunden> gefunden = Unterlagenpruefung.dateienIn(
                "Anlage_1_TP5_V22_20260521.pdf Anlage_1_TP5_V22_20260521.pdf");

        assertEquals(1, gefunden.size());
    }

    /** Eine Unterlage ohne Versionsnummer wird ebenfalls erkannt. */
    @Test
    @DisplayName("erkennt auch Dateien ohne Versionsnummer")
    void ohneVersion() {
        List<Unterlagenpruefung.Gefunden> gefunden =
                Unterlagenpruefung.dateienIn("Anhang_1_Datenuebermittlung_20170831.pdf");

        assertEquals(1, gefunden.size());
        assertEquals(java.time.LocalDate.of(2017, 8, 31), gefunden.get(0).stand());
    }
}
