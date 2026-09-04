package de.gkvtransmitter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
