package de.gkvtransmitter.dta;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;

class DtaFactoryTest {

    @Test
    void buildDtaForCreatesReferenceAlignedMessages() {
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345, 108310400, 101560000, LocalDate.of(1990, 1, 1));
        patient.setId(1);
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321, 104940005, 101560000, LocalDate.of(1985, 2, 2));
        provider.setId(2);
        Blueprint blueprint = Testblaupause.mitPreis();
        Abrechnung abrechnung = new Abrechnung(patient, provider, blueprint, 3);

        String dta = DtaFactory.buildDtaFor(abrechnung, 1L, String.valueOf(provider.getIk()), String.valueOf(patient.getKassenIk()));

        assertTrue(dta.contains("UNB+UNOC:3+104940005+101560000+"));
        // Sechstes Element: der Leistungsbereich. F = Hebammen, abgeleitet aus
        // dem Abrechnungscode 50 - dort stand bis zum 07.09.2026 fest ein H.
        // Siebtes: der logische Dateiname nach Anhang 1, Abschnitt 4.2.
        assertTrue(dta.contains("+00001+F+SL494000S"), dta);
        assertTrue(dta.contains("UNH+00001+SLGA:21:0:0'"));
        assertTrue(dta.contains("UNH+00002+SLLA:21:0:0'"));
        assertTrue(dta.contains("UST+19'"));
        assertTrue(dta.contains("GES+00+45000,00+45000,00'"));
        assertTrue(dta.contains("GES+99+45000,00+45000,00'"));
        assertTrue(dta.contains("INV+000000000001"));
        assertTrue(dta.contains("NAD+BEISPIEL+ANNA+19900101+MUSTERSTRASSE 1+12345+ORT'"));
        assertTrue(dta.contains("ENF+01+50:00000+306050601+3,00+15000,00+"));
        assertTrue(dta.contains("BES+45000,00'"));
        assertTrue(dta.contains("UNZ+000002+00001'"));
    }

    /**
     * Prueft, dass die Angaben aus einer Blaupause die erzeugte Nachricht
     * tatsaechlich erreichen.
     *
     * <p>Der Test oben arbeitet mit einer leeren Blaupause und traf deshalb
     * immer die Vorbelegung. Dass der Weg von der gespeicherten Blaupause bis
     * in die Nachricht ueberhaupt nicht durchgaengig war, konnte er nicht
     * bemerken - und bemerkte es jahrelang nicht.</p>
     */
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("Was in der Blaupause steht,")
    class AusDerBlaupause {

        private String dtaMitBlaupause(String felder, int termine) {
            Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                    108310400, 101560000, LocalDate.of(1990, 1, 1));
            patient.setId(1);
            ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321,
                    104940005, 101560000, LocalDate.of(1985, 2, 2));
            provider.setId(2);
            Blueprint blaupause = new Blueprint("Kurs", "test-template",
                    "{\"template\":\"test\",\"fields\":{" + felder + "}}", OffsetDateTime.now());
            Abrechnung abrechnung = new Abrechnung(patient, provider, blaupause, termine);
            return DtaFactory.buildDtaFor(abrechnung, 1L,
                    String.valueOf(provider.getIk()), String.valueOf(patient.getKassenIk()));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("bestimmt den Rechnungsbetrag statt der Vorbelegung")
        void bestimmtDenRechnungsbetrag() {
            // Ein Kurspreis, wie er wirklich vorkommt - nicht die 15.000,00
            // aus der Beispieldatei. Vier Termine zu 12,50 ergeben 50,00.
            String dta = dtaMitBlaupause("\"Durchschnittlicher Einzelbetrag\":\"12,50\"", 4);

            assertTrue(dta.contains("+4,00+12,50+"), "Einzelbetrag fehlt in der Leistungszeile:\n" + dta);
            assertTrue(dta.contains("BES+50,00'"), "Fallsumme falsch:\n" + dta);
            assertTrue(dta.contains("GES+00+50,00+50,00'"), "Gesamtsumme falsch:\n" + dta);
            assertTrue(dta.contains("GES+99+50,00+50,00'"), "Gesamtsumme falsch:\n" + dta);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("bestimmt Abrechnungscode und Tarifkennzeichen")
        void bestimmtDieLeistungserbringergruppe() {
            String dta = dtaMitBlaupause(
                    "\"Abrechnungscode\":\"65\",\"Tarifkennzeichen\":\"12345\"", 1);

            assertTrue(dta.contains("ENF+01+65:12345+"), "Leistungserbringergruppe falsch:\n" + dta);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("bestimmt den Umsatzsteuersatz")
        void bestimmtDenUmsatzsteuersatz() {
            String dta = dtaMitBlaupause("\"Umsatzsteuersatz\":\"7\"", 1);

            assertTrue(dta.contains("UST+7'"), "Umsatzsteuersatz falsch:\n" + dta);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("bestimmt die Abrechnungspositionsnummer")
        void bestimmtDiePositionsnummer() {
            String dta = dtaMitBlaupause("\"Abrechnungspositionsnummer\":\"306050699\"", 1);

            assertTrue(dta.contains("+306050699+"), "Positionsnummer falsch:\n" + dta);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("laesst die Vorbelegung greifen, wo sie nichts angibt")
        void laesstDieVorbelegungGreifen() {
            String dta = dtaMitBlaupause("\"Umsatzsteuersatz\":\"7\"", 1);

            assertTrue(dta.contains("ENF+01+50:00000+306050601+"),
                    "Vorbelegung greift nicht mehr:\n" + dta);
        }
    }

    /**
     * Der logische Dateiname im UNB.
     *
     * <p>Er hiess bis zum 07.09.2026 {@code HEB} plus Datum plus laufende
     * Nummer - elf Stellen, aber frei erfunden. Anhang 1 zur Anlage 1,
     * Abschnitt 4.2 gibt ihn genau vor, siehe {@link LogischerDateiname}.</p>
     */
    @Test
    @org.junit.jupiter.api.DisplayName("Der logische Dateiname folgt Anhang 1, nicht der Belegnummer")
    void logischerDateinameFolgtDerVorgabe() {
        String ref = LogischerDateiname.bilde("261914007", true, java.time.LocalDate.of(2026, 5, 30));

        assertEquals(11, ref.length());
        assertEquals("SL191400S05", ref);
    }
}
