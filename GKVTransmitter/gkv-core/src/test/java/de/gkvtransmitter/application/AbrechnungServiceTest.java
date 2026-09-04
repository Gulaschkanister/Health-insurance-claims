package de.gkvtransmitter.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
        BillingOfficeEndpoint endpoint = BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse",
                tempDirectory.resolve("endpoint"));
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(
                Map.of(108310400, endpoint), tempDirectory.resolve("fallback"));
        AbrechnungService service = new AbrechnungService(
                new DtaDispatchService(registry, new FileBillingOfficeTransport()));

        List<DispatchBatch> batches = service.createAndDispatch(
                List.of(patient), group, blueprint, Map.of(1, 2), tempDirectory);

        assertEquals(1, batches.size());
        assertTrue(Files.list(tempDirectory.resolve("endpoint")).findAny().isPresent());
    }

    @Test
    void rejectsNegativeAppointmentCount() {
        Patient patient = patient(1, 108310400);
        PersonGroup group = new PersonGroup("Kurs");
        group.getServiceProviders().add(provider(2));
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
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
