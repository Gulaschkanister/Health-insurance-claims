package de.gkvtransmitter.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.dispatch.BillingOfficeEndpoint;
import de.gkvtransmitter.dispatch.BillingOfficeEndpointRegistry;
import de.gkvtransmitter.dispatch.DispatchBatch;
import de.gkvtransmitter.dispatch.DtaDispatchService;
import de.gkvtransmitter.dispatch.FileBillingOfficeTransport;
import de.gkvtransmitter.dta.Testblaupause;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.PersonGroup;
import de.gkvtransmitter.entity.ServiceProvider;

class AbrechnungServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsAndDispatchesSettlementForSelectedPatients() throws Exception {
        Patient patient = patient(1, 108310400);
        ServiceProvider provider = provider(2);
        PersonGroup group = new PersonGroup("Kurs");
        group.getPatients().add(patient);
        group.getServiceProviders().add(provider);
        Blueprint blueprint = Testblaupause.mitPreis();
        BillingOfficeEndpoint endpoint = BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse",
                tempDirectory.resolve("endpoint"));
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(
                Map.of(108310400, endpoint), tempDirectory.resolve("fallback"));
        AbrechnungService service = new AbrechnungService(
                new DtaDispatchService(registry, new FileBillingOfficeTransport()));

        List<DispatchBatch> batches = service.createAndDispatch(
                List.of(patient), group, blueprint, Map.of(1, 2), tempDirectory).lieferungen();

        assertEquals(1, batches.size());
        assertTrue(Files.list(tempDirectory.resolve("endpoint")).findAny().isPresent());
    }

    /**
     * Ein Trockenlauf hinterlaesst nichts.
     *
     * <p>Das ist die eigentliche Schwierigkeit an D1, und hier wird sie
     * gemessen, wo sie zaehlt: an einer echten Datenbank. Das
     * Uebermittlungsprotokoll ist Pflichtdokumentation nach Anlage 1,
     * Abschnitt 3 Absatz 2 und zwei Jahre aufzubewahren - <b>eine Uebung darin
     * waere eine Falschaussage</b>. Der Versand daneben muss dagegen
     * schreiben, sonst prueft dieser Test nur, dass ueberhaupt nichts
     * geschieht.</p>
     */
    @Test
    void probelaufHinterlaesstKeinenProtokolleintrag() throws Exception {
        Path dbDatei = tempDirectory.resolve("probelauf.db");
        try (de.gkvtransmitter.hibernate.sqllite.HibernateSqllite datenbank =
                de.gkvtransmitter.hibernate.sqllite.HibernateSqllite.open(
                        de.gkvtransmitter.hibernate.sqllite.DatabaseSettings.forFile(dbDatei))) {
            Patient patient = patient(1, 108310400);
            PersonGroup group = new PersonGroup("Kurs");
            group.getPatients().add(patient);
            group.getServiceProviders().add(provider(2));
            Blueprint blueprint = Testblaupause.mitPreis();
            AbrechnungService service = new AbrechnungService(datenbank);

            service.probelauf(List.of(patient), group, blueprint, Map.of(1, 2));

            assertTrue(datenbank.ladeProtokoll().isEmpty(),
                    "Eine Uebung gehoert nicht in die Pflichtdokumentation");

            service.createAndDispatch(List.of(patient), group, blueprint, Map.of(1, 2),
                    tempDirectory.resolve("echt"));

            assertEquals(1, datenbank.ladeProtokoll().size(),
                    "Der echte Versand muss dagegen schreiben - sonst misst der Test nichts");

            // Und der Eintrag muss als Ursprung einer Korrektur taugen: Ohne
            // Rechnungsnummer und Rechnungsdatum stuende im URI-Segment
            // nichts, worauf die Kasse zurueckfinden kann.
            de.gkvtransmitter.entity.Protokolleintrag eintrag = datenbank.ladeProtokoll().get(0);
            assertTrue(eintrag.getRechnungsnummer() > 0, "Rechnungsnummer fehlt");
            assertNotNull(eintrag.getRechnungsdatum(), "Rechnungsdatum fehlt");
            assertTrue(eintrag.korrigierbar(),
                    "Eine angenommene Lieferung muss sich korrigieren lassen"
                            + " - Rechnungsnummer " + eintrag.getRechnungsnummer()
                            + ", Datum " + eintrag.getRechnungsdatum()
                            + ", fehlerfrei " + eintrag.istFehlerfrei()
                            + ", Richtung " + eintrag.getRichtung());
        }
    }

    /**
     * Der Trockenlauf prueft dasselbe, was hinausginge.
     *
     * <p>Beide Wege stellen die Abrechnungen ueber dieselbe Methode zusammen.
     * Zwei, die auseinanderliefen, ergaeben einen Trockenlauf, der etwas
     * anderes durchspielt als den Versand - also keinen.</p>
     */
    @Test
    void probelaufBeanstandetDasselbeWieDerVersand() {
        Patient patient = patient(1, 108310400);
        PersonGroup group = new PersonGroup("Kurs");
        group.getPatients().add(patient);
        group.getServiceProviders().add(provider(2));
        AbrechnungService service = new AbrechnungService(
                new DtaDispatchService(new BillingOfficeEndpointRegistry(
                        new LinkedHashMap<>(), tempDirectory.resolve("fallback")),
                        new FileBillingOfficeTransport()));

        de.gkvtransmitter.dispatch.Trockenlauf.Ergebnis ergebnis = service.probelauf(
                List.of(patient), group, Testblaupause.ohnePreis(), Map.of(1, 2));

        assertTrue(ergebnis.bericht().hatFehler(), ergebnis.bericht().alsText());
        assertThrows(de.gkvtransmitter.dispatch.DtaValidierungsException.class,
                () -> service.createAndDispatch(List.of(patient), group, Testblaupause.ohnePreis(),
                        Map.of(1, 2), tempDirectory),
                "Was der Trockenlauf beanstandet, muss auch den Versand aufhalten");
    }

    @Test
    void rejectsNegativeAppointmentCount() {
        Patient patient = patient(1, 108310400);
        PersonGroup group = new PersonGroup("Kurs");
        group.getServiceProviders().add(provider(2));
        Blueprint blueprint = Testblaupause.mitPreis();
        AbrechnungService service = new AbrechnungService(new DtaDispatchService(
                new BillingOfficeEndpointRegistry(new LinkedHashMap<>(), tempDirectory.resolve("fallback")),
                new FileBillingOfficeTransport()));

        assertThrows(IllegalArgumentException.class, () -> service.createAndDispatch(
                List.of(patient), group, blueprint, Map.of(1, -1), tempDirectory));
    }

    private Patient patient(int id, int kassenIk) {
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                108310400, kassenIk, LocalDate.of(1990, 1, 1));
        patient.setId(id);
        return patient;
    }

    private ServiceProvider provider(int id) {
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321,
                104940005, 108310400, LocalDate.of(1985, 2, 2));
        provider.setId(id);
        return provider;
    }
}
