package de.gkvtransmitter.dta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;

/**
 * Prueft, wofuer sich eine erzeugte Datei ausgibt.
 *
 * <p>Die letzte Stelle des UNB entscheidet darueber, was die Datenannahmestelle
 * mit der Lieferung macht: 0 Test, 1 Erprobung, 2 Echt (Technische Anlage,
 * Abschnitt 5.4). <b>Bis zum 07.09.2026 stand dort fest eine 1</b> - jede
 * Datei war eine Erprobungsdatei, und niemand konnte das sehen oder aendern.
 * Eine Echtabrechnung waere angenommen, verarbeitet und nicht bezahlt
 * worden.</p>
 */
@DisplayName("Uebermittlungsart")
class UebermittlungsartTest {

    @Test
    @DisplayName("Die Kennzeichen entsprechen der Technischen Anlage")
    void kennzeichen() {
        assertEquals("0", Uebermittlungsart.TEST.kennzeichen());
        assertEquals("1", Uebermittlungsart.ERPROBUNG.kennzeichen());
        assertEquals("2", Uebermittlungsart.ECHT.kennzeichen());
    }

    /**
     * Im Zweifel keine Forderung.
     *
     * <p>Ein Vertipper in der Einstellungsdatei darf nicht dazu fuehren, dass
     * echte Forderungen hinausgehen. Andersherum kostet er einen zweiten
     * Versand - das ist die guenstigere Seite des Irrtums.</p>
     */
    @Test
    @DisplayName("Was sich nicht zuordnen laesst, wird zur Erprobung")
    void unbekanntWirdErprobung() {
        assertEquals(Uebermittlungsart.ERPROBUNG, Uebermittlungsart.aus("echtbetrieb-vielleicht"));
        assertEquals(Uebermittlungsart.ERPROBUNG, Uebermittlungsart.aus(""));
        assertEquals(Uebermittlungsart.ERPROBUNG, Uebermittlungsart.aus(null));
    }

    @Test
    @DisplayName("Gross- und Kleinschreibung sind egal")
    void schreibweise() {
        assertEquals(Uebermittlungsart.ECHT, Uebermittlungsart.aus("ECHT"));
        assertEquals(Uebermittlungsart.ECHT, Uebermittlungsart.aus(" echt "));
        assertEquals("echt", Uebermittlungsart.ECHT.alsEinstellung());
    }

    @Test
    @DisplayName("Die gewaehlte Art steht an der letzten Stelle des UNB")
    void artStehtImUnb() {
        for (Uebermittlungsart art : Uebermittlungsart.values()) {
            String unb = unbVon(dtaMit(art));

            assertTrue(unb.endsWith("+" + art.kennzeichen() + "'"),
                    art + " muss im UNB als " + art.kennzeichen() + " stehen: " + unb);
        }
    }

    @Test
    @DisplayName("Ohne Angabe bleibt es bei der Erprobung")
    void ohneAngabeErprobung() {
        String unb = unbVon(DtaFactory.buildDtaFor(abrechnung(), 1L, "104940005", "101560000"));

        assertTrue(unb.endsWith("+1'"), unb);
    }

    private String unbVon(String dta) {
        return dta.lines().findFirst().orElseThrow();
    }

    private String dtaMit(Uebermittlungsart art) {
        return DtaFactory.buildDtaFor(abrechnung(), 1L, "104940005", "101560000",
                Leistungsparameter.ausBlueprint(null), art);
    }

    private Abrechnung abrechnung() {
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                108310400, 101560000, LocalDate.of(1990, 1, 1));
        patient.setId(1);
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321,
                104940005, 101560000, LocalDate.of(1985, 2, 2));
        provider.setId(2);
        Blueprint blaupause = new Blueprint("Test", "test-template",
                Testblaupause.MIT_PREIS, OffsetDateTime.now());
        return new Abrechnung(patient, provider, blaupause, 3);
    }
}
