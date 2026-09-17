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

/**
 * Das {@code UST}-Segment traegt keinen Umsatzsteuersatz.
 *
 * <p><b>Bis zum 17.09.2026 schrieb die Anwendung {@code UST+19'}.</b> Nach
 * Anlage 1, Abschnitt 5.5.2 - gleichlautend in Version 21 und 22 - besteht das
 * Segment aus dem Kennzeichen {@code UST}, der <b>Steuernummer</b> nach § 14
 * Abs. 1a UStG (hoechstens 20 Stellen, innerhalb des Segments Pflicht) und
 * einer Kennung der Befreiung ({@code J}, wenn befreit gem. § 4 UStG). Die 19
 * landete damit im Feld der Steuernummer.</p>
 *
 * <p>Sie stammt aus {@code Information/Valide.DTA}, das an derselben Stelle
 * {@code UST+19} fuehrt - dieselbe Referenzdatei, die dem Projekt schon den
 * Abrechnungscode 61 und den Leistungsbereich H eingetragen hat.</p>
 */
@DisplayName("Das UST-Segment")
class UmsatzsteuersegmentTest {

    private static ServiceProvider erbringer() {
        ServiceProvider erbringer = new ServiceProvider("Maria", "Hebamme", "Kurweg", "DE", "3",
                28203, 261914007, 101560000, LocalDate.of(1980, 1, 9));
        erbringer.setId(2);
        return erbringer;
    }

    private static String dtaFuer(ServiceProvider erbringer) {
        Patient anna = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                108310400, 101560000, LocalDate.of(1990, 1, 1));
        anna.setId(1);
        Abrechnung abrechnung = new Abrechnung(anna, erbringer, Testblaupause.mitPreis(), 1);
        return DtaFactory.buildDtaFor(abrechnung, 1L,
                String.valueOf(erbringer.getIk()), String.valueOf(anna.getKassenIk()));
    }

    @Test
    @DisplayName("traegt Steuernummer und Befreiungskennzeichen")
    void steuernummerUndBefreiung() {
        ServiceProvider erbringer = erbringer();
        erbringer.setSteuernummer("12/345/67890");
        erbringer.setUmsatzsteuerbefreit(true);

        assertTrue(dtaFuer(erbringer).contains("UST+12/345/67890+J'"),
                "Steuernummer und J nach § 4 UStG:\n" + dtaFuer(erbringer));
    }

    /** Ohne Befreiung bleibt die dritte Stelle leer, das Feld ist konditional. */
    @Test
    @DisplayName("laesst die Kennung weg, wenn keine Befreiung vorliegt")
    void ohneBefreiung() {
        ServiceProvider erbringer = erbringer();
        erbringer.setSteuernummer("12/345/67890");

        assertTrue(dtaFuer(erbringer).contains("UST+12/345/67890+'"),
                "Ohne Befreiung bleibt die Kennung leer:\n" + dtaFuer(erbringer));
    }

    /**
     * Ohne Steuernummer bleibt das ganze Segment weg.
     *
     * <p>Die Steuernummer ist innerhalb des Segments Pflicht, das Segment
     * selbst ist konditional (Segmentart {@code K}, Wiederholungsfaktor 0-1).
     * Weglassen ist also erlaubt - eine erfundene Nummer waere es nicht. Das
     * ist dieselbe Entscheidung wie beim fehlenden Geburtsdatum im
     * {@code NAD}.</p>
     */
    @Test
    @DisplayName("bleibt ganz weg, wenn keine Steuernummer hinterlegt ist")
    void ohneSteuernummerKeinSegment() {
        String dta = dtaFuer(erbringer());

        assertFalse(dta.contains("UST+"), "Erfinden ist die schlechtere Antwort:\n" + dta);
    }

    /**
     * Die Segmentzahl im {@code UNT} stimmt in beiden Faellen.
     *
     * <p>Das {@code UNT} nennt die Zahl der Segmente der Nachricht, sich selbst
     * eingeschlossen. Faellt ein Segment weg, muss die Zahl mitgehen - sonst
     * schlaegt {@code NachrichtenAbschlussRegel} an.</p>
     */
    @Test
    @DisplayName("aendert die Segmentzahl im UNT mit")
    void segmentzahlStimmt() {
        ServiceProvider mitNummer = erbringer();
        mitNummer.setSteuernummer("12/345/67890");

        assertTrue(dtaFuer(mitNummer).contains("UNT+000008+00001'"),
                "Mit UST sind es acht Segmente:\n" + dtaFuer(mitNummer));
        assertTrue(dtaFuer(erbringer()).contains("UNT+000007+00001'"),
                "Ohne UST sind es sieben:\n" + dtaFuer(erbringer()));
    }

    /** Leerraum um die Steuernummer gehoert nicht in die Nachricht. */
    @Test
    @DisplayName("schneidet Leerraum um die Steuernummer ab")
    void schneidetLeerraumAb() {
        ServiceProvider erbringer = erbringer();
        erbringer.setSteuernummer("  12/345/67890  ");

        assertEquals("UST+12/345/67890+'",
                DtaFactory.umsatzsteuersegment(erbringer).orElseThrow());
    }
}
