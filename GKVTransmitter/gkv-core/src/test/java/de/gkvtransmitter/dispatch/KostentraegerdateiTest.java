package de.gkvtransmitter.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Der Leser der Kostentraegerdatei.
 *
 * <p>Geprueft wird gegen selbst gebaute Daten nach
 * {@code Information/Anhang_3_Kostentraegerdatei_V10_20260414.pdf} - die echte,
 * vierteljaehrlich erneuerte Datei liegt dem Projekt nicht vor und gehoert in
 * den Wartungsplan, nicht in einen Test.</p>
 *
 * <p><b>Der Punkt, um den es geht:</b> Zugestellt wird an die
 * Datenannahmestelle <em>mit</em> Entschluesselungsbefugnis (Verknuepfungsart
 * 03), nicht an die Kasse und nicht an einen Netzbetreiber ohne Befugnis
 * (Verknuepfungsart 02).</p>
 */
class KostentraegerdateiTest {

    private static final String KASSE = "108310400";
    private static final String KOSTENTRAEGER = "104940005";
    private static final String ANNAHMESTELLE = "102137985";
    private static final String NETZBETREIBER = "101560000";

    /** Hebamme/Entbindungspfleger, Anlage 3 Abschnitt 8.1.5. */
    private static final String HEBAMME = "50";

    /**
     * Eine Datei mit dem vollen Weg: Karte -&gt; Kostentraeger -&gt;
     * Annahmestelle, und daneben ein Netzbetreiber ohne Befugnis.
     */
    private static String volleDatei() {
        return String.join("\n",
                "UNB+UNOC:3+ABSENDER+EMPFAENGER+20260414:1200+00001'",
                "IDK+" + KASSE + "+19+Musterkasse Karte'",
                "VKG+01+" + KOSTENTRAEGER + "'",
                "IDK+" + KOSTENTRAEGER + "+19+Musterkasse Kostentraeger'",
                // Zuerst der Netzbetreiber OHNE Befugnis - er darf nicht gewinnen.
                "VKG+02+" + NETZBETREIBER + "+++++++" + HEBAMME + "'",
                "VKG+03+" + ANNAHMESTELLE + "+++++++" + HEBAMME + "'",
                "IDK+" + ANNAHMESTELLE + "+99+Annahmestelle Nord'",
                "DFU+01+070++++ +annahme@beispiel.de'",
                "UNZ+000001+00001'");
    }

    @Nested
    @DisplayName("Den Empfaenger finden")
    class Empfaenger {

        @Test
        @DisplayName("Der Weg fuehrt ueber den Kostentraeger zur Annahmestelle")
        void findetAnnahmestelle() {
            Optional<Annahmestelle> gefunden =
                    Kostentraegerdatei.lies(volleDatei()).annahmestelleFuer(108310400, HEBAMME);

            assertTrue(gefunden.isPresent(), "Die Annahmestelle muss auffindbar sein");
            assertEquals(102137985, gefunden.get().ik());
            assertEquals("Annahmestelle Nord", gefunden.get().name());
        }

        @Test
        @DisplayName("Eine Stelle ohne Entschluesselungsbefugnis gewinnt nicht")
        void uebergehtNetzbetreiber() {
            Annahmestelle gefunden = Kostentraegerdatei.lies(volleDatei())
                    .annahmestelleFuer(108310400, HEBAMME).orElseThrow();

            assertFalse(gefunden.ik() == 101560000,
                    "Verknuepfungsart 02 ist ein Netzbetreiber ohne Befugnis - dorthin darf nichts");
        }

        @Test
        @DisplayName("Der Uebertragungsweg kommt aus dem DFU-Segment")
        void liestUebertragungsweg() {
            Annahmestelle gefunden = Kostentraegerdatei.lies(volleDatei())
                    .annahmestelleFuer(108310400, HEBAMME).orElseThrow();

            assertEquals(Annahmestelle.PROTOKOLL_EMAIL, gefunden.protokoll());
            assertEquals("annahme@beispiel.de", gefunden.kanal());
            assertTrue(gefunden.hatUebertragungsweg());
        }

        /**
         * Traegt die Karte schon das IK des Kostentraegers, fehlt der Verweis
         * 01 - dann steht die Annahmestelle unmittelbar an diesem IDK.
         */
        @Test
        @DisplayName("Auch ohne Verweis auf den Kostentraeger wird gefunden")
        void findetOhneZwischenschritt() {
            String datei = String.join("\n",
                    "IDK+" + KASSE + "+19+Kasse ist zugleich Kostentraeger'",
                    "VKG+03+" + ANNAHMESTELLE + "+++++++" + HEBAMME + "'",
                    "IDK+" + ANNAHMESTELLE + "+99+Annahmestelle Nord'",
                    "DFU+01+070++++ +annahme@beispiel.de'");

            assertEquals(102137985,
                    Kostentraegerdatei.lies(datei).annahmestelleFuer(108310400, HEBAMME)
                            .orElseThrow().ik());
        }
    }

    @Nested
    @DisplayName("Der Abrechnungscode entscheidet mit")
    class Abrechnungscode {

        private String mitCode(String code) {
            return String.join("\n",
                    "IDK+" + KASSE + "+19+Kasse'",
                    "VKG+03+" + ANNAHMESTELLE + "+++++++" + code + "'",
                    "IDK+" + ANNAHMESTELLE + "+99+Annahmestelle'",
                    "DFU+01+070++++ +annahme@beispiel.de'");
        }

        @Test
        @DisplayName("Der Sammelschluessel 00 gilt fuer alle Leistungen")
        void sammelschluesselGiltImmer() {
            assertTrue(Kostentraegerdatei.lies(mitCode("00"))
                    .annahmestelleFuer(108310400, HEBAMME).isPresent());
        }

        @Test
        @DisplayName("Ein Gruppenschluessel deckt seine Einzelschluessel ab")
        void gruppenschluesselDecktEinzelne() {
            // Fussnote 4: Einzelschluessel 47 liegt unter Gruppenschluessel 40.
            assertTrue(Kostentraegerdatei.lies(mitCode("40"))
                    .annahmestelleFuer(108310400, "47").isPresent());
        }

        @Test
        @DisplayName("Ein fremder Abrechnungscode fuehrt nicht zu dieser Stelle")
        void fremderCodeFindetNichts() {
            assertTrue(Kostentraegerdatei.lies(mitCode("61"))
                    .annahmestelleFuer(108310400, HEBAMME).isEmpty(),
                    "61 ist Rehabilitationssport - dafuer gilt dieser Verweis nicht");
        }

        /**
         * Der Sonderschluessel 99 gilt fuer <em>nicht aufgefuehrte</em>
         * Gruppen. Ihn auf einen aufgefuehrten Code anzuwenden hiesse, an eine
         * Stelle zu liefern, die dafuer nicht benannt ist.
         */
        @Test
        @DisplayName("Der Sonderschluessel 99 wird nicht stillschweigend benutzt")
        void sonderschluesselGiltNichtFuerAlles() {
            assertTrue(Kostentraegerdatei.lies(mitCode("99"))
                    .annahmestelleFuer(108310400, HEBAMME).isEmpty());
        }
    }

    @Nested
    @DisplayName("Was fehlt, wird nicht erfunden")
    class Luecken {

        @Test
        @DisplayName("Eine unbekannte Kasse liefert nichts")
        void unbekannteKasse() {
            assertTrue(Kostentraegerdatei.lies(volleDatei())
                    .annahmestelleFuer(999999999, HEBAMME).isEmpty());
        }

        @Test
        @DisplayName("Eine leere Datei liefert nichts und stuerzt nicht ab")
        void leereDatei() {
            Kostentraegerdatei datei = Kostentraegerdatei.lies("");

            assertTrue(datei.istLeer());
            assertTrue(datei.annahmestelleFuer(108310400, HEBAMME).isEmpty());
        }

        @Test
        @DisplayName("Ein Verweis ins Leere bleibt brauchbar, aber ohne Weg")
        void verweisOhneBlock() {
            String datei = String.join("\n",
                    "IDK+" + KASSE + "+19+Kasse'",
                    "VKG+03+" + ANNAHMESTELLE + "+++++++" + HEBAMME + "'");

            Annahmestelle gefunden = Kostentraegerdatei.lies(datei)
                    .annahmestelleFuer(108310400, HEBAMME).orElseThrow();

            assertEquals(102137985, gefunden.ik());
            assertFalse(gefunden.hatUebertragungsweg(),
                    "Ohne IDK-Block der Annahmestelle gibt es weder Namen noch Kanal");
        }
    }
}
