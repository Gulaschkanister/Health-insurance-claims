package de.gkvtransmitter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prueft die Bestimmung des Datenverzeichnisses.
 *
 * <p>Der Kern dieser Tests ist die Zusicherung, dass kein Pfad mehr vom
 * Arbeitsverzeichnis abhaengt. Genau daran haette eine ausgelieferte Anwendung
 * gescheitert: gestartet aus {@code C:\Program Files} waere sie nicht
 * schreibberechtigt gewesen, gestartet vom Desktop haette sie ihre Daten dort
 * abgelegt.</p>
 */
class AnwendungsverzeichnisTest {

    @AfterEach
    void aufraeumen() {
        System.clearProperty(Anwendungsverzeichnis.BASIS_PROPERTY);
    }

    /**
     * Der Umzug vom alten Ordnernamen.
     *
     * <p>Bis zum 06.09.2026 hiess der Datenordner {@code GKVTransmitter}. Eine
     * Umbenennung ohne Umzug haette die bestehende Datenbank am alten Ort
     * liegen lassen, und die Anwendung waere mit leeren Listen aufgegangen -
     * fuer jemanden mit einem Jahr Stammdaten ist das von Datenverlust nicht
     * zu unterscheiden.</p>
     */
    @Nested
    @DisplayName("Der Umzug vom alten Ordnernamen")
    class Umzug {

        @Test
        @DisplayName("nimmt die Daten mit")
        void nimmtDieDatenMit(@TempDir Path wurzel) throws Exception {
            Path alt = wurzel.resolve("GKVTransmitter");
            Files.createDirectories(alt);
            Files.writeString(alt.resolve("database.db"), "stammdaten");
            Path neu = wurzel.resolve("GKV-Abrechnung");

            Path ergebnis = Anwendungsverzeichnis.umgezogen(neu, alt);

            assertEquals(neu, ergebnis);
            assertEquals("stammdaten", Files.readString(neu.resolve("database.db")));
            assertFalse(Files.exists(alt), "Sonst gaebe es die Daten zweimal");
        }

        @Test
        @DisplayName("laesst einen bestehenden neuen Ordner unberuehrt")
        void ueberschreibtNichts(@TempDir Path wurzel) throws Exception {
            Path alt = wurzel.resolve("GKVTransmitter");
            Files.createDirectories(alt);
            Files.writeString(alt.resolve("database.db"), "alt");
            Path neu = wurzel.resolve("GKV-Abrechnung");
            Files.createDirectories(neu);
            Files.writeString(neu.resolve("database.db"), "neu");

            assertEquals(neu, Anwendungsverzeichnis.umgezogen(neu, alt));
            assertEquals("neu", Files.readString(neu.resolve("database.db")),
                    "Ein zweiter Stand darf einen bestehenden nicht verdraengen");
        }

        @Test
        @DisplayName("tut nichts, wenn es nichts umzuziehen gibt")
        void ohneAltenOrdner(@TempDir Path wurzel) {
            Path neu = wurzel.resolve("GKV-Abrechnung");

            assertEquals(neu, Anwendungsverzeichnis.umgezogen(neu, wurzel.resolve("GKVTransmitter")));
        }

        /**
         * Die wichtigste Eigenschaft: scheitert der Umzug, wird weiter am
         * <em>alten</em> Ort gearbeitet.
         *
         * <p>Mit einem leeren neuen Ordner aufzugehen waere das Schlimmste -
         * die Daten waeren noch da, aber niemand saehe sie, und man finge an,
         * alles noch einmal einzugeben. Hier steht am Zielort eine Datei
         * statt eines Ordners; das Verschieben scheitert.</p>
         */
        @Test
        @DisplayName("arbeitet am alten Ort weiter, wenn er scheitert")
        void bleibtBeimAltenWennEsNichtGeht(@TempDir Path wurzel) throws Exception {
            Path alt = wurzel.resolve("GKVTransmitter");
            Files.createDirectories(alt);
            Files.writeString(alt.resolve("database.db"), "stammdaten");
            // Ein Ziel, dessen Elternordner es nicht gibt: das Verschieben
            // scheitert, ohne dass vorher schon exists(neu) greift. Genau
            // dieser Weg muss zum alten Ort zurueckfuehren.
            Path neu = wurzel.resolve("gibt-es-nicht").resolve("GKV-Abrechnung");

            Path ergebnis = Anwendungsverzeichnis.umgezogen(neu, alt);

            assertEquals(alt, ergebnis,
                    "Mit einem leeren neuen Ordner aufzugehen waere von Datenverlust "
                            + "nicht zu unterscheiden");
            assertEquals("stammdaten", Files.readString(alt.resolve("database.db")));
        }
    }

    @Test
    @DisplayName("Ohne Vorgabe liegt das Verzeichnis ausserhalb des Arbeitsverzeichnisses")
    void haengtNichtAmArbeitsverzeichnis() {
        System.clearProperty(Anwendungsverzeichnis.BASIS_PROPERTY);

        Path basis = Anwendungsverzeichnis.basis();
        Path arbeitsverzeichnis = Paths.get("").toAbsolutePath();

        assertTrue(basis.isAbsolute(), "Der Pfad muss absolut sein, war: " + basis);
        assertTrue(!basis.startsWith(arbeitsverzeichnis),
                "Das Datenverzeichnis darf nicht unterhalb des Arbeitsverzeichnisses liegen: " + basis);
    }

    @Test
    @DisplayName("Die Systemeigenschaft gkv.home hat Vorrang")
    void beachtetSystemeigenschaft() {
        Path gewuenscht = Paths.get(System.getProperty("java.io.tmpdir"), "gkv-test-basis").toAbsolutePath();
        System.setProperty(Anwendungsverzeichnis.BASIS_PROPERTY, gewuenscht.toString());

        assertEquals(gewuenscht, Anwendungsverzeichnis.basis());
    }

    @Test
    @DisplayName("Datenbank und Versandordner liegen unterhalb des Datenverzeichnisses")
    void legtUnterhalbDerBasisAb() {
        Path gewuenscht = Paths.get(System.getProperty("java.io.tmpdir"), "gkv-test-basis").toAbsolutePath();
        System.setProperty(Anwendungsverzeichnis.BASIS_PROPERTY, gewuenscht.toString());

        assertEquals(gewuenscht.resolve("database.db"), Anwendungsverzeichnis.datenbank());
        assertEquals(gewuenscht.resolve("dta_output"), Anwendungsverzeichnis.versandordner());
    }

    @Test
    @DisplayName("Ein absoluter Pfad wird unveraendert uebernommen")
    void laesstAbsolutenPfadUnveraendert() {
        Path absolut = Paths.get(System.getProperty("java.io.tmpdir"), "irgendwo").toAbsolutePath();

        assertEquals(absolut, Anwendungsverzeichnis.aufloesen(absolut));
    }

    @Test
    @DisplayName("Ein relativer Pfad wird gegen das Datenverzeichnis aufgeloest")
    void loestRelativenPfadAuf() {
        Path gewuenscht = Paths.get(System.getProperty("java.io.tmpdir"), "gkv-test-basis").toAbsolutePath();
        System.setProperty(Anwendungsverzeichnis.BASIS_PROPERTY, gewuenscht.toString());

        // So sind die Zielordner in billing-office-endpoints.json notiert.
        Path aufgeloest = Anwendungsverzeichnis.aufloesen(Paths.get("dta_output", "outbox", "aok-bayern"));

        assertEquals(gewuenscht.resolve("dta_output").resolve("outbox").resolve("aok-bayern"), aufgeloest);
        assertTrue(aufgeloest.isAbsolute());
    }

    @Test
    @DisplayName("null ergibt das Datenverzeichnis selbst")
    void behandeltNull() {
        Path gewuenscht = Paths.get(System.getProperty("java.io.tmpdir"), "gkv-test-basis").toAbsolutePath();
        System.setProperty(Anwendungsverzeichnis.BASIS_PROPERTY, gewuenscht.toString());

        assertEquals(gewuenscht, Anwendungsverzeichnis.aufloesen(null));
    }
}
