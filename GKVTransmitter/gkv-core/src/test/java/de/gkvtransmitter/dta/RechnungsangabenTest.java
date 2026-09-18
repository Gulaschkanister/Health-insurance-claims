package de.gkvtransmitter.dta;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Rechnungsnummer und Belegnummer.
 *
 * <p>Beide standen fest im Quelltext: {@code REC+00000000:0} und eine frei
 * gebildete Belegnummer {@code HEB} + Jahr/Monat + Kennnummer. Die Kasse
 * haette jede Lieferung unter derselben Rechnungsnummer verbucht - und eine
 * Korrektur darauf verwiese ins Leere.</p>
 */
@DisplayName("Rechnungs- und Belegnummer")
class RechnungsangabenTest {

    private final DtaValidationService pruefung = DtaValidationService.standard();

    private static String dta(Rechnungsangaben rechnung) {
        ServiceProvider erbringer = new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE", "3",
                28203, 261914007, 101560000, LocalDate.of(1980, 1, 9));
        erbringer.setId(2);
        Patient anna = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                108310400, 108310400, LocalDate.of(1990, 1, 1));
        anna.setId(1);
        Abrechnung abrechnung = new Abrechnung(anna, erbringer, Testblaupause.mitPreis(), 2);
        return DtaFactory.buildDtaFor(abrechnung, 1L, "261914007", "108310400",
                Leistungsparameter.ausBlueprint(abrechnung.getBlueprint()),
                Uebermittlungsart.ERPROBUNG, Absender.ausLeistungserbringer("261914007"), rechnung);
    }

    @Test
    @DisplayName("stehen im REC und im INV")
    void stehenInDerNachricht() {
        String nachricht = dta(new Rechnungsangaben(42L, 1));

        assertTrue(nachricht.contains("REC+00000042:0+"), nachricht);
        assertTrue(nachricht.contains("+1'"), nachricht);
    }

    /**
     * Die Einzel-Rechnungsnummer bleibt {@code 0}.
     *
     * <p>Anlage 1, Abschnitt 5.5.2: Sie wird nur gefuellt, wenn eine
     * Abrechnungsstelle mit Inkassovollmacht absendet; sonst "wird lediglich
     * das Datenelement Sammel-Rechnungsnummer gefuellt und die
     * Einzel-Rechnungsnummer auf 0 gesetzt".</p>
     */
    @Test
    @DisplayName("lassen die Einzel-Rechnungsnummer auf 0")
    void einzelnummerBleibtNull() {
        assertEquals("00000042:0", new Rechnungsangaben(42L, 1).rechnungsnummerFeld());
    }

    /**
     * Die Nullnummer wird zurueckgewiesen.
     *
     * <p><b>Das ist der Ausgang der Sperre.</b> {@code Rechnungsangaben.OHNE}
     * erzeugt genau die {@code 00000000}, die bis zum 18.09.2026 fest
     * dastand - und eine Regel haelt sie jetzt auf, statt sie durchzulassen.
     * Dieselbe Entscheidung wie beim Einzelbetrag, der auf {@code 0,00} faellt
     * statt auf einen plausiblen Betrag.</p>
     */
    @Test
    @DisplayName("Eine Rechnungsnummer aus lauter Nullen haelt den Lauf auf")
    void nullnummerWirdBeanstandet() {
        ValidationReport bericht = pruefung.pruefe(dta(Rechnungsangaben.OHNE));

        assertTrue(bericht.getErrors().stream()
                        .anyMatch(befund -> "RECHNUNGSNUMMER_NULL".equals(befund.code())),
                bericht.alsText());
        assertFalse(bericht.istVersandfaehig());
    }

    @Test
    @DisplayName("Eine vergebene Nummer geht durch")
    void vergebeneNummerGehtDurch() {
        ValidationReport bericht = pruefung.pruefe(dta(new Rechnungsangaben(42L, 1)));

        assertFalse(bericht.getErrors().stream()
                        .anyMatch(befund -> befund.code().startsWith("RECHNUNGSNUMMER")
                                || befund.code().startsWith("BELEGNUMMER")),
                bericht.alsText());
    }

    /**
     * Die Belegnummer muss nur innerhalb der Rechnung eindeutig sein.
     *
     * <p>Anlage 1, Abschnitt 5.5.2: "Eindeutige Nummer innerhalb der
     * Gesamtrechnung". <b>Das ist weniger, als die Aufgabenliste annahm</b> -
     * sie sprach von einer Sicherung "ueber Jahresgrenzen". Eindeutig ueber
     * die Zeit ist die Rechnungsnummer.</p>
     */
    @Test
    @DisplayName("Dieselbe Belegnummer zweimal in einer Rechnung wird beanstandet")
    void doppelteBelegnummer() {
        String einfach = dta(new Rechnungsangaben(42L, 1));
        // Ein zweites INV mit derselben Belegnummer daneben.
        String doppelt = einfach.replace("NAD+BEISPIEL", "INV+000000000002++1+1'\nNAD+BEISPIEL");

        ValidationReport bericht = pruefung.pruefe(doppelt);

        assertTrue(bericht.getErrors().stream()
                        .anyMatch(befund -> "BELEGNUMMER_DOPPELT".equals(befund.code())),
                bericht.alsText());
    }

    /** Zwei Belege derselben Rechnung zaehlen hoch. */
    @Test
    @DisplayName("Der naechste Beleg behaelt die Rechnungsnummer")
    void naechsterBeleg() {
        Rechnungsangaben zweiter = new Rechnungsangaben(42L, 1).naechsterBeleg();

        assertEquals(42L, zweiter.rechnungsnummer());
        assertEquals("2", zweiter.belegnummerFeld());
    }
}
