package de.gkvtransmitter.einstellung;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prueft die Einstellungsdatei.
 *
 * <p>Sie ist die erste Stelle, an der die Anwendung sich etwas merkt, das keine
 * Fachdatei ist. Wichtiger als das Speichern selbst ist deshalb, <b>was
 * geschieht, wenn es schiefgeht</b>: eine unlesbare Datei darf den Start nicht
 * aufhalten, und ein abgebrochenes Schreiben darf die vorige Fassung nicht
 * mitnehmen.</p>
 *
 * <p>Hier ist {@code @TempDir} unbedenklich - anders als bei der Datenbank
 * bleibt keine Verbindung offen, die Windows am Loeschen hindern wuerde.</p>
 */
@DisplayName("Einstellungen")
class EinstellungenTest {

    @Test
    @DisplayName("Ohne Datei gelten die Vorgaben")
    void ohneDatei(@TempDir Path ordner) {
        Einstellungen einstellungen = Einstellungen.laden(ordner.resolve("gibt-es-nicht.json"));

        assertEquals("hell", einstellungen.get(Einstellung.DARSTELLUNG));
        assertEquals("erprobung", einstellungen.get(Einstellung.UEBERMITTLUNGSART));
    }

    @Test
    @DisplayName("Was gesetzt wurde, steht beim naechsten Laden wieder da")
    void ueberlebtDenNeustart(@TempDir Path ordner) {
        Path datei = ordner.resolve(Einstellungen.DATEINAME);

        assertTrue(Einstellungen.laden(datei).setze(Einstellung.DARSTELLUNG, "dunkel"));

        assertEquals("dunkel", Einstellungen.laden(datei).get(Einstellung.DARSTELLUNG),
                "Eine Einstellung, die den Neustart nicht uebersteht, ist keine");
    }

    @Test
    @DisplayName("Ein leerer Wert stellt die Vorgabe wieder her")
    void leerSetztZurueck(@TempDir Path ordner) {
        Path datei = ordner.resolve(Einstellungen.DATEINAME);
        Einstellungen einstellungen = Einstellungen.laden(datei);
        einstellungen.setze(Einstellung.DARSTELLUNG, "dunkel");

        einstellungen.setze(Einstellung.DARSTELLUNG, null);

        assertEquals("hell", Einstellungen.laden(datei).get(Einstellung.DARSTELLUNG));
    }

    @Test
    @DisplayName("Die Datei wird angelegt, wenn es den Ordner noch nicht gibt")
    void legtOrdnerAn(@TempDir Path ordner) throws IOException {
        Path datei = ordner.resolve("noch").resolve("nicht").resolve(Einstellungen.DATEINAME);

        assertTrue(Einstellungen.laden(datei).setze(Einstellung.DARSTELLUNG, "dunkel"));

        assertTrue(Files.isRegularFile(datei), "Beim ersten Start gibt es den Ordner noch nicht");
    }

    /**
     * Der Fall, der zaehlt.
     *
     * <p>Eine beschaedigte Einstellungsdatei darf die Anwendung nicht am
     * Starten hindern. Sie enthaelt eine Farbwahl - nichts, wofuer jemand vor
     * einem Programm sitzen sollte, das nicht aufgeht.</p>
     */
    @Nested
    @DisplayName("Wenn die Datei kaputt ist")
    class Kaputt {

        @Test
        @DisplayName("Unlesbarer Inhalt fuehrt zu den Vorgaben, nicht zum Abbruch")
        void unlesbar(@TempDir Path ordner) throws IOException {
            Path datei = ordner.resolve(Einstellungen.DATEINAME);
            Files.writeString(datei, "{das ist kein json");

            assertEquals("hell", Einstellungen.laden(datei).get(Einstellung.DARSTELLUNG));
        }

        @Test
        @DisplayName("Ein fremder Aufbau fuehrt zu den Vorgaben")
        void falscherAufbau(@TempDir Path ordner) throws IOException {
            Path datei = ordner.resolve(Einstellungen.DATEINAME);
            Files.writeString(datei, "[\"eine Liste statt eines Objekts\"]");

            assertEquals("hell", Einstellungen.laden(datei).get(Einstellung.DARSTELLUNG));
        }

        @Test
        @DisplayName("Ein unbekannter Schluessel stoert nicht")
        void unbekannterSchluessel(@TempDir Path ordner) throws IOException {
            Path datei = ordner.resolve(Einstellungen.DATEINAME);
            Files.writeString(datei, "{\"darstellung\":\"dunkel\",\"was-auch-immer\":\"x\"}");

            assertEquals("dunkel", Einstellungen.laden(datei).get(Einstellung.DARSTELLUNG),
                    "Eine spaetere Fassung darf mehr hineinschreiben, ohne diese zu brechen");
        }

        /**
         * Ein gescheitertes Schreiben meldet sich, statt zu werfen.
         *
         * <p>Nachgestellt ueber einen Ordner, den es nicht geben kann: der
         * Elternpfad ist eine Datei. Die Oberflaeche muss darauf hinweisen
         * koennen ("nicht gespeichert"), und eine Ausnahme mitten im Klick auf
         * einen Umschalter waere das Gegenteil davon.</p>
         *
         * <p>Der erste Anlauf dieses Tests legte stattdessen einen leeren
         * Ordner am Zielpfad an - und {@code Files.move} ersetzte ihn
         * anstandslos. <b>Ein Test, der einen Fehler nachstellen soll, muss
         * nachrechnen, dass es wirklich einer ist.</b></p>
         */
        @Test
        @DisplayName("Ein gescheitertes Schreiben wirft nicht, sondern meldet sich")
        void schreibenScheitert(@TempDir Path ordner) throws IOException {
            Path keinOrdner = ordner.resolve("eine-datei");
            Files.writeString(keinOrdner, "ich bin ein Ordner, der keiner ist");

            assertFalse(Einstellungen.laden(keinOrdner.resolve(Einstellungen.DATEINAME))
                    .setze(Einstellung.DARSTELLUNG, "dunkel"));
        }
    }
}
