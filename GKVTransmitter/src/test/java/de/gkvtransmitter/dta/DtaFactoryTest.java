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
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345, 111111111, 987654321, LocalDate.of(1990, 1, 1));
        patient.setId(1);
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321, 222222222, 987654321, LocalDate.of(1985, 2, 2));
        provider.setId(2);
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
        Abrechnung abrechnung = new Abrechnung(patient, provider, blueprint, 3);

        String dta = DtaFactory.buildDtaFor(abrechnung, 1L, String.valueOf(provider.getIk()), String.valueOf(patient.getKassenIk()));

        assertTrue(dta.contains("UNB+UNOC:3+222222222+987654321+"));
        assertTrue(dta.contains("+00001+H+HEB"));
        assertTrue(dta.contains("UNH+00001+SLGA:21:0:0'"));
        assertTrue(dta.contains("UNH+00002+SLLA:21:0:0'"));
        assertTrue(dta.contains("UST+19'"));
        assertTrue(dta.contains("GES+00+45000,00+45000,00'"));
        assertTrue(dta.contains("GES+99+45000,00+45000,00'"));
        assertTrue(dta.contains("INV+000000000001"));
        assertTrue(dta.contains("NAD+BEISPIEL+ANNA+19900101+MUSTERSTRASSE 1+12345+ORT'"));
        assertTrue(dta.contains("ENF+01+61:00000+306050601+3,00+15000,00+"));
        assertTrue(dta.contains("BES+45000,00'"));
        assertTrue(dta.contains("UNZ+000002+00001'"));
    }

    @Test
    void applicationReferenceHasElevenCharacters() {
        String ref = DtaFactory.buildApplicationRef(java.time.LocalDateTime.of(2026, 5, 30, 20, 17), 12);
        assertEquals(11, ref.length());
        assertTrue(ref.startsWith("HEB"));
    }
}