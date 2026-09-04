package de.gkvtransmitter.dispatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;

class DtaDispatchServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesFilesGroupedByKassenIk() throws Exception {
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321, 104940005, 101560000, LocalDate.of(1985, 2, 2));
        provider.setId(10);
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
        Patient patient1 = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345, 108310400, 108310400, LocalDate.of(1990, 1, 1));
        patient1.setId(1);
        Patient patient2 = new Patient("Ben", "Beispiel", "Musterstrasse", "DE", "3", 12345, 102137985, 104940005, LocalDate.of(1991, 2, 2));
        patient2.setId(2);

        List<Abrechnung> abrechnungen = List.of(
                new Abrechnung(patient1, provider, blueprint, 1),
                new Abrechnung(patient2, provider, blueprint, 2));

        Map<Integer, BillingOfficeEndpoint> endpoints = new LinkedHashMap<>();
        endpoints.put(108310400, BillingOfficeEndpoint.fileEndpoint(108310400, "Kasse A", tempDir.resolve("send-a")));
        endpoints.put(104940005, BillingOfficeEndpoint.fileEndpoint(104940005, "Kasse B", tempDir.resolve("send-b")));
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(endpoints, tempDir.resolve("fallback"));

        DtaDispatchService service = new DtaDispatchService(registry, new FileBillingOfficeTransport());
        List<DispatchBatch> batches = service.generateAndRoute(abrechnungen, tempDir);

        assertEquals(2, batches.size());
        assertTrue(Files.exists(tempDir.resolve("send-a")));
        assertTrue(Files.exists(tempDir.resolve("send-b")));
        try (var filesA = Files.list(tempDir.resolve("send-a")); var filesB = Files.list(tempDir.resolve("send-b"))) {
            assertEquals(1, filesA.count());
            assertEquals(1, filesB.count());
        }
    }

    @Test
    void testDtaArrivesAtConfiguredEndpoint() throws Exception {
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321, 104940005, 101560000, LocalDate.of(1985, 2, 2));
        provider.setId(10);
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345, 108310400, 108310400, LocalDate.of(1990, 1, 1));
        patient.setId(1);

        List<Abrechnung> abrechnungen = List.of(new Abrechnung(patient, provider, blueprint, 1));

        Path endpointDir = tempDir.resolve("endpoint");
        Map<Integer, BillingOfficeEndpoint> endpoints = new LinkedHashMap<>();
        endpoints.put(108310400, BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse", endpointDir));
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(endpoints, tempDir.resolve("fallback"));

        DtaDispatchService service = new DtaDispatchService(registry, new FileBillingOfficeTransport());
        List<DispatchBatch> batches = service.generateAndRoute(abrechnungen, tempDir);

        assertEquals(1, batches.size());
        Path deliveredFile = batches.get(0).getFiles().get(0);

        try (var stagedFiles = Files.list(tempDir.resolve("staging").resolve("108310400"));
             var endpointFiles = Files.list(endpointDir)) {
            Path stagedFile = stagedFiles.findFirst().orElseThrow();
            Path endpointFile = endpointFiles.findFirst().orElseThrow();

            assertTrue(Files.exists(stagedFile));
            assertTrue(Files.exists(deliveredFile));
            assertEquals(Files.readString(stagedFile), Files.readString(deliveredFile));
            assertEquals(Files.readString(stagedFile), Files.readString(endpointFile));
        }
    }

    @Test
    void parsesResponseTypes() {
        DtaDispatchService service = new DtaDispatchService();

        assertEquals(BillingOfficeResponseType.SYNTAX_ERROR, service.parseResponse("Die Datei konnte erfolgreich entschlüsselt werden. Typ: PLAIN_EDIFACT syntaxFehler").getType());
        assertEquals(BillingOfficeResponseType.ACCEPTED, service.parseResponse("Annahme erfolgreich").getType());
        assertEquals(BillingOfficeResponseType.REJECTED, service.parseResponse("abgelehnt wegen Fehler").getType());
        assertEquals(BillingOfficeResponseType.UNKNOWN, service.parseResponse(null).getType());
    }

    @Test
    void probesConfiguredEndpoints() {
        Map<Integer, BillingOfficeEndpoint> endpoints = new LinkedHashMap<>();
        endpoints.put(333333333, BillingOfficeEndpoint.fileEndpoint(333333333, "Probe-Kasse", tempDir.resolve("probe")));
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(endpoints, tempDir.resolve("fallback"));

        DtaDispatchService service = new DtaDispatchService(registry, new FileBillingOfficeTransport());
        List<BillingOfficeEndpointCheck> checks = service.checkConfiguredEndpoints();

        assertEquals(1, checks.size());
        assertEquals(333333333, checks.get(0).kassenIk());
        assertTrue(checks.get(0).reachable());
        assertTrue(Files.exists(tempDir.resolve("probe")));
    }

    @Test
    void probesEveryConfiguredDefaultEndpoint() {
        DtaDispatchService service = new DtaDispatchService();

        List<BillingOfficeEndpointCheck> checks = service.checkConfiguredEndpoints();

        assertFalse(checks.isEmpty());
        assertTrue(checks.stream().allMatch(BillingOfficeEndpointCheck::reachable));
    }
}