package de.gkvtransmitter.dta;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Laesst die Validierung auf das los, was die {@link DtaFactory} tatsaechlich
 * erzeugt.
 *
 * <p>Die vorhandenen Tests der Fabrik vergleichen einzelne Zeichenketten im
 * Ergebnis. Das sagt nichts darueber aus, ob die Nachricht als Ganzes stimmig
 * ist - Segmentzaehler und Summen muessen zueinander passen, nicht nur je fuer
 * sich richtig aussehen. Genau das prueft dieser Test.</p>
 *
 * <p>Verwendet werden echte Kassen-IK aus der Endpunktliste, weil die
 * Pruefziffer mitgeprueft wird. Die Beispiel-IK der aelteren Tests
 * ({@code 222222222}, {@code 987654321}) haben keine gueltige Pruefziffer und
 * waeren im Echtbetrieb abgewiesen worden.</p>
 */
class DtaFactoryValidierungTest {

    /** AOK Bayern, Pruefziffer geprueft. */
    private static final int KASSEN_IK = 108310400;
    /** BARMER, hier als Absender-IK des Leistungserbringers. */
    private static final int LEISTUNGSERBRINGER_IK = 104940005;

    private final DtaValidationService validierung = DtaValidationService.standard();

    private static Abrechnung abrechnung(int termine) {
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1",
                12345, 111111111, KASSEN_IK, LocalDate.of(1990, 1, 1));
        patient.setId(1);
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2",
                54321, LEISTUNGSERBRINGER_IK, KASSEN_IK, LocalDate.of(1985, 2, 2));
        provider.setId(2);
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
        return new Abrechnung(patient, provider, blueprint, termine);
    }

    private String erzeuge(int termine) {
        return DtaFactory.buildDtaFor(abrechnung(termine), 1L,
                String.valueOf(LEISTUNGSERBRINGER_IK), String.valueOf(KASSEN_IK));
    }

    @Test
    @DisplayName("Eine regulaer erzeugte Abrechnung ist versandfaehig")
    void erzeugteAbrechnungIstVersandfaehig() {
        ValidationReport bericht = validierung.pruefe(erzeuge(3));

        assertTrue(bericht.istVersandfaehig(),
                "Die erzeugte Nachricht wurde beanstandet:\n" + bericht.alsText());
    }

    @Test
    @DisplayName("Auch ein einzelner Termin ergibt eine stimmige Nachricht")
    void einzelnerTerminIstVersandfaehig() {
        ValidationReport bericht = validierung.pruefe(erzeuge(1));

        assertTrue(bericht.istVersandfaehig(), bericht.alsText());
    }

    @Test
    @DisplayName("Viele Termine ergeben eine stimmige Nachricht")
    void vieleTermineSindVersandfaehig() {
        ValidationReport bericht = validierung.pruefe(erzeuge(48));

        assertTrue(bericht.istVersandfaehig(), bericht.alsText());
    }

    @Test
    @DisplayName("Segmentzaehler und Rahmen der erzeugten Nachricht stimmen")
    void zaehlerStimmen() {
        DtaDocument dokument = DtaDocument.parse(erzeuge(5));

        assertTrue(dokument.anzahlMitTag("UNH") == 2,
                "Erwartet werden eine SLGA- und eine SLLA-Nachricht");
        assertTrue(dokument.anzahlMitTag("UNT") == 2);
        assertTrue(dokument.anzahlMitTag("UNB") == 1);
        assertTrue(dokument.anzahlMitTag("UNZ") == 1);
    }

    @Test
    @DisplayName("Eine Abrechnung ohne Termine erzeugt keine widerspruechliche Summe")
    void abrechnungOhneTermineIstStimmig() {
        // AbrechnungService laesst 0 Termine zu - es wird nur auf negative
        // Werte geprueft. Die erzeugte Nachricht muss deshalb auch dann in sich
        // stimmig sein, sonst weist die Kasse die Rechnung zurueck.
        ValidationReport bericht = validierung.pruefe(erzeuge(0));

        assertTrue(bericht.istVersandfaehig(),
                "Bei 0 Terminen entsteht eine Nachricht, deren Summen nicht zusammenpassen:\n"
                        + bericht.alsText());
    }
}
