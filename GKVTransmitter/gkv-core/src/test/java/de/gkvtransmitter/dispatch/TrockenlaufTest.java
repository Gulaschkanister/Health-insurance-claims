package de.gkvtransmitter.dispatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.dta.Testblaupause;
import de.gkvtransmitter.entity.Anschrift;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;

/**
 * Einmal durchspielen, ohne zu senden.
 *
 * <p>{@code SimulierteKassenGegenstelle} und {@code SimulierterKassenTransport}
 * gab es seit Langem - erreichbar waren sie nur aus Tests. Wer vor dem ersten
 * Echtversand wissen wollte, ob eine Lieferung durchkommt, musste einen Test
 * schreiben.</p>
 *
 * <p>Die wichtigsten Tests hier pruefen nicht, <em>dass</em> es geht, sondern
 * <b>dass nichts zurueckbleibt</b>.</p>
 */
@DisplayName("Der Trockenlauf")
class TrockenlaufTest {

    private static Abrechnung abrechnung(int termine) {
        ServiceProvider erbringer = new ServiceProvider("Maria", "Hebamme", new Anschrift("Kurweg", "3", 28203, "DE"), 261914007, 101560000, LocalDate.of(1980, 1, 9));
        erbringer.setId(2);
        erbringer.setAbrechnungscode("50");
        Blueprint blaupause = Testblaupause.mitPreis();
        Patient anna = new Patient("Anna", "Beispiel", new Anschrift("Musterstrasse", "1", 12345, "DE"), 108310400, 108310400, LocalDate.of(1990, 1, 1));
        anna.setId(1);
        return new Abrechnung(anna, erbringer, blaupause, termine);
    }

    private static Trockenlauf.Ergebnis spiele(Abrechnung... abrechnungen) {
        return new Trockenlauf(() -> null).spiele(List.of(abrechnungen));
    }

    @Test
    @DisplayName("erzeugt eine Datei und holt sich ein Urteil der Gegenstelle")
    void spieltDurch() {
        Trockenlauf.Ergebnis ergebnis = spiele(abrechnung(4));

        assertTrue(ergebnis.bestanden(), ergebnis.bericht().alsText());
        assertEquals(1, ergebnis.dateien());
        assertFalse(ergebnis.antworten().isEmpty(), "Ohne Urteil waere der Lauf sinnlos");
    }

    /**
     * Keine verbrauchte Datenaustauschreferenz.
     *
     * <p>Jede darf nur einmal vergeben werden. Wer sie im Trockenlauf
     * verbraucht, hinterlaesst Luecken in der echten Folge - und
     * "Datenaustauschreferenz doppelt vergeben" ist ein dokumentierter
     * Abweisungsgrund, eine Luecke dagegen eine Frage der Kasse.</p>
     */
    @Test
    @DisplayName("beginnt jedes Mal wieder bei der ersten Referenz")
    void verbrauchtKeineReferenz() throws Exception {
        assertEquals("00001", referenzAus(spiele(abrechnung(1))));
        assertEquals("00001", referenzAus(spiele(abrechnung(1))),
                "Der zweite Lauf darf nicht weiterzaehlen - er teilt sich die Folge mit niemandem");
    }

    /** Die Datenaustauschreferenz aus dem UNB der erzeugten Datei. */
    private static String referenzAus(Trockenlauf.Ergebnis ergebnis) throws Exception {
        try (var dateien = Files.walk(ergebnis.ordner())) {
            Path datei = dateien.filter(Files::isRegularFile)
                    .filter(pfad -> pfad.getFileName().toString().endsWith(".dta"))
                    .findFirst().orElseThrow();
            return Files.readString(datei).split("\n")[0].split("\\+")[5];
        }
    }

    /**
     * Die Datei weist sich als Test aus.
     *
     * <p>Die letzte Stelle des {@code UNB} sagt, wofuer sich die Datei
     * ausgibt: 0 Test, 1 Erprobung, 2 Echt. Sollte eine Uebungsdatei je
     * irgendwo landen, soll sie es selbst sagen.</p>
     */
    @Test
    @DisplayName("erzeugt Dateien, die sich als Test ausweisen")
    void dateienSindTestdateien() throws Exception {
        Trockenlauf.Ergebnis ergebnis = spiele(abrechnung(1));

        try (var dateien = Files.walk(ergebnis.ordner())) {
            Path datei = dateien.filter(Files::isRegularFile)
                    .filter(pfad -> pfad.getFileName().toString().endsWith(".dta"))
                    .findFirst().orElseThrow();
            assertTrue(Files.readString(datei).split("\n")[0].endsWith("+0'"),
                    "Letzte Stelle des UNB: 0 = Test");
        }
    }

    /** Zwei Laeufe mischen sich nicht. */
    @Test
    @DisplayName("nimmt fuer jeden Lauf ein eigenes Verzeichnis")
    void eigenesVerzeichnisJeLauf() {
        assertFalse(spiele(abrechnung(1)).ordner().equals(spiele(abrechnung(1)).ordner()));
    }

    /**
     * Eine nicht bestandene Pruefung ist das Ergebnis, kein Fehlschlag.
     *
     * <p>Genau dafuer spielt man durch - der Trockenlauf wirft deshalb nicht,
     * sondern gibt den Bericht zurueck.</p>
     */
    @Test
    @DisplayName("gibt bei beanstandeter Pruefung den Bericht zurueck, statt zu werfen")
    void beanstandungOhneAusnahme() {
        Abrechnung ohnePreis = new Abrechnung(abrechnung(1).getPatient(), abrechnung(1).getProvider(),
                Testblaupause.ohnePreis(), 1);

        Trockenlauf.Ergebnis ergebnis = spiele(ohnePreis);

        assertFalse(ergebnis.bestanden());
        assertTrue(ergebnis.bericht().hatFehler(), ergebnis.bericht().alsText());
        assertEquals(0, ergebnis.dateien(), "Es darf nichts entstanden sein");
    }
}
