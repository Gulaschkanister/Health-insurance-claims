package de.gkvtransmitter.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Prueft den Weg von der Abrechnung bis zur Rueckmeldung der Kasse.
 *
 * <p>Verwendet werden echte Kassen-IK, weil die Pruefziffer Teil der
 * Validierung ist und der Versand sonst zu Recht abgelehnt wird.</p>
 */
class KassenKommunikationTest {

    /** AOK Bayern. */
    private static final int KASSEN_IK = 108310400;
    /** BARMER, hier als IK des Leistungserbringers. */
    private static final int ERBRINGER_IK = 104940005;
    /** Ein IK mit falscher Pruefziffer. */
    private static final int UNGUELTIGES_IK = 108310401;

    @TempDir
    Path tempDir;

    private static Abrechnung abrechnung(int patientId, int kassenIk, int termine) {
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1",
                12345, ERBRINGER_IK, kassenIk, LocalDate.of(1990, 1, 1));
        patient.setId(patientId);
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2",
                54321, ERBRINGER_IK, kassenIk, LocalDate.of(1985, 2, 2));
        provider.setId(99);
        Blueprint blueprint = new Blueprint("Test", "test-template", "{}", OffsetDateTime.now());
        return new Abrechnung(patient, provider, blueprint, termine);
    }

    private BillingOfficeEndpointRegistry registry(int kassenIk, Path ziel) {
        Map<Integer, BillingOfficeEndpoint> endpoints = new LinkedHashMap<>();
        endpoints.put(kassenIk, BillingOfficeEndpoint.fileEndpoint(kassenIk, "Test-Kasse", ziel));
        return new BillingOfficeEndpointRegistry(endpoints, tempDir.resolve("fallback"));
    }

    @Nested
    @DisplayName("Pruefung vor dem Versand")
    class Pruefstufe {

        @Test
        @DisplayName("Eine fehlerhafte Abrechnung wird nicht versendet")
        void haeltFehlerhafteAbrechnungAuf() {
            Path ziel = tempDir.resolve("endpoint");
            DtaDispatchService service = new DtaDispatchService(
                    registry(UNGUELTIGES_IK, ziel), new FileBillingOfficeTransport());

            assertThrows(DtaValidierungsException.class, () -> service.generateAndRoute(
                    List.of(abrechnung(1, UNGUELTIGES_IK, 2)), tempDir));

            assertFalse(Files.exists(ziel), "Bei fehlerhafter Pruefung darf nichts zugestellt werden");
        }

        @Test
        @DisplayName("Eine einzige fehlerhafte Abrechnung haelt den gesamten Lauf auf")
        void haeltGesamtenLaufAuf() throws Exception {
            Path ziel = tempDir.resolve("endpoint");
            Map<Integer, BillingOfficeEndpoint> endpoints = new LinkedHashMap<>();
            endpoints.put(KASSEN_IK, BillingOfficeEndpoint.fileEndpoint(KASSEN_IK, "Gut", ziel));
            endpoints.put(UNGUELTIGES_IK, BillingOfficeEndpoint.fileEndpoint(UNGUELTIGES_IK, "Schlecht", ziel));
            DtaDispatchService service = new DtaDispatchService(
                    new BillingOfficeEndpointRegistry(endpoints, tempDir.resolve("fallback")),
                    new FileBillingOfficeTransport());

            assertThrows(DtaValidierungsException.class, () -> service.generateAndRoute(
                    List.of(abrechnung(1, KASSEN_IK, 2), abrechnung(2, UNGUELTIGES_IK, 1)), tempDir));

            // Die erste Abrechnung waere fuer sich genommen in Ordnung gewesen.
            // Sie darf trotzdem nicht zugestellt sein, sonst muesste sie bei der
            // Kasse einzeln storniert werden.
            assertFalse(Files.exists(ziel),
                    "Es darf nichts zugestellt sein, solange eine Abrechnung des Laufs beanstandet wird");
        }

        @Test
        @DisplayName("Der Pruefbericht liegt der Ausnahme bei")
        void liefertBerichtMit() {
            DtaDispatchService service = new DtaDispatchService(
                    registry(UNGUELTIGES_IK, tempDir.resolve("endpoint")), new FileBillingOfficeTransport());

            DtaValidierungsException fehler = assertThrows(DtaValidierungsException.class,
                    () -> service.generateAndRoute(List.of(abrechnung(1, UNGUELTIGES_IK, 2)), tempDir));

            assertNotNull(fehler.getBericht());
            assertTrue(fehler.getBericht().hatFehler());
            assertTrue(fehler.getBericht().getErrors().stream()
                    .anyMatch(m -> m.code().equals("IK_PRUEFZIFFER")), fehler.getBericht().alsText());
        }

        @Test
        @DisplayName("Pruefen ohne Versenden stellt nichts zu")
        void pruefenVersendetNicht() {
            Path ziel = tempDir.resolve("endpoint");
            DtaDispatchService service = new DtaDispatchService(
                    registry(KASSEN_IK, ziel), new FileBillingOfficeTransport());

            ValidationReport bericht = service.pruefe(List.of(abrechnung(1, KASSEN_IK, 2)));

            assertTrue(bericht.istVersandfaehig(), bericht.alsText());
            assertFalse(Files.exists(ziel), "Das Pruefen allein darf nichts zustellen");
        }

        @Test
        @DisplayName("Eine korrekte Abrechnung passiert die Pruefung und wird zugestellt")
        void stelltKorrekteAbrechnungZu() throws Exception {
            Path ziel = tempDir.resolve("endpoint");
            DtaDispatchService service = new DtaDispatchService(
                    registry(KASSEN_IK, ziel), new FileBillingOfficeTransport());

            List<DispatchBatch> batches = service.generateAndRoute(
                    List.of(abrechnung(1, KASSEN_IK, 2)), tempDir);

            assertEquals(1, batches.size());
            try (var dateien = Files.list(ziel)) {
                assertEquals(1, dateien.count());
            }
        }
    }

    @Nested
    @DisplayName("Simulierte Kasse")
    class Gegenstelle {

        private final SimulierteKassenGegenstelle kasse = new SimulierteKassenGegenstelle("AOK Testkasse");

        @Test
        @DisplayName("Eine korrekte Lieferung wird angenommen")
        void nimmtKorrekteLieferungAn() {
            String dta = de.gkvtransmitter.dta.DtaFactory.buildDtaFor(
                    abrechnung(1, KASSEN_IK, 3), 1L, String.valueOf(ERBRINGER_IK), String.valueOf(KASSEN_IK));

            BillingOfficeResponse antwort = kasse.empfange(dta);

            assertEquals(BillingOfficeResponseType.ACCEPTED, antwort.getType(), antwort.getRawContent());
            assertTrue(antwort.getRawContent().contains("AOK Testkasse"));
        }

        @Test
        @DisplayName("Eine leere Lieferung wird als Syntaxfehler zurueckgewiesen")
        void weistLeereLieferungZurueck() {
            BillingOfficeResponse antwort = kasse.empfange("");

            assertEquals(BillingOfficeResponseType.SYNTAX_ERROR, antwort.getType());
        }

        @Test
        @DisplayName("Ein Strukturfehler wird als Syntaxfehler zurueckgemeldet")
        void meldetStrukturfehlerAlsSyntaxfehler() {
            String dta = de.gkvtransmitter.dta.DtaFactory.buildDtaFor(
                    abrechnung(1, KASSEN_IK, 3), 1L, String.valueOf(ERBRINGER_IK), String.valueOf(KASSEN_IK));
            String kaputt = dta.replaceFirst("UNZ\\+\\d+", "UNZ+000009");

            BillingOfficeResponse antwort = kasse.empfange(kaputt);

            assertEquals(BillingOfficeResponseType.SYNTAX_ERROR, antwort.getType(), antwort.getRawContent());
        }

        @Test
        @DisplayName("Ein inhaltlicher Fehler wird als Ablehnung zurueckgemeldet")
        void meldetInhaltsfehlerAlsAblehnung() {
            String dta = de.gkvtransmitter.dta.DtaFactory.buildDtaFor(
                    abrechnung(1, KASSEN_IK, 3), 1L, String.valueOf(UNGUELTIGES_IK), String.valueOf(KASSEN_IK));

            BillingOfficeResponse antwort = kasse.empfange(dta);

            assertEquals(BillingOfficeResponseType.REJECTED, antwort.getType(), antwort.getRawContent());
            assertTrue(antwort.getRawContent().contains("IK_PRUEFZIFFER"), antwort.getRawContent());
        }

        @Test
        @DisplayName("Das Antwortprotokoll nennt die Beanstandungen einzeln")
        void protokollNenntBeanstandungen() {
            String dta = de.gkvtransmitter.dta.DtaFactory.buildDtaFor(
                    abrechnung(1, KASSEN_IK, 3), 1L, String.valueOf(UNGUELTIGES_IK), String.valueOf(KASSEN_IK));

            String protokoll = kasse.empfange(dta).getRawContent();

            assertTrue(protokoll.contains("Beanstandungen:"), protokoll);
            assertTrue(protokoll.contains("zurueckgewiesen"), protokoll);
        }

        @Test
        @DisplayName("Die Antwort laesst sich mit dem Auswerter wieder einordnen")
        void antwortIstAuswertbar() {
            String dta = de.gkvtransmitter.dta.DtaFactory.buildDtaFor(
                    abrechnung(1, KASSEN_IK, 3), 1L, String.valueOf(ERBRINGER_IK), String.valueOf(KASSEN_IK));
            String protokoll = kasse.empfange(dta).getRawContent();

            BillingOfficeResponse eingeordnet = new BillingOfficeResponseParser().parse(protokoll);

            assertEquals(BillingOfficeResponseType.ACCEPTED, eingeordnet.getType(), protokoll);
        }
    }

    @Nested
    @DisplayName("Transport mit Rueckmeldung")
    class TransportMitAntwort {

        @Test
        @DisplayName("Der Versand erzeugt neben der Datei ein Antwortprotokoll")
        void erzeugtAntwortprotokoll() throws Exception {
            Path ziel = tempDir.resolve("endpoint");
            SimulierterKassenTransport transport = new SimulierterKassenTransport();
            DtaDispatchService service = new DtaDispatchService(registry(KASSEN_IK, ziel), transport);

            service.generateAndRoute(List.of(abrechnung(1, KASSEN_IK, 2)), tempDir);

            try (var dateien = Files.list(ziel)) {
                List<Path> alle = dateien.toList();
                assertEquals(2, alle.size(), "erwartet werden die DTA-Datei und ihr Antwortprotokoll");
                assertTrue(alle.stream().anyMatch(p -> p.toString().endsWith(
                        SimulierteKassenGegenstelle.PROTOKOLL_ENDUNG)));
            }
        }

        @Test
        @DisplayName("Die Rueckmeldung der Kasse ist danach abrufbar")
        void haeltRueckmeldungBereit() {
            Path ziel = tempDir.resolve("endpoint");
            SimulierterKassenTransport transport = new SimulierterKassenTransport();
            DtaDispatchService service = new DtaDispatchService(registry(KASSEN_IK, ziel), transport);

            service.generateAndRoute(List.of(abrechnung(1, KASSEN_IK, 2)), tempDir);

            BillingOfficeResponse antwort = transport.letzteAntwort(KASSEN_IK);
            assertNotNull(antwort, "Nach dem Versand muss eine Rueckmeldung vorliegen");
            assertEquals(BillingOfficeResponseType.ACCEPTED, antwort.getType(), antwort.getRawContent());
        }

        @Test
        @DisplayName("Der umschliessende Transport verhaelt sich sonst wie der eingepackte")
        void verhaeltSichWieDelegat() {
            SimulierterKassenTransport transport = new SimulierterKassenTransport();

            assertEquals(BillingOfficeTransportType.FILE, transport.getType());
            assertTrue(transport.probe(BillingOfficeEndpoint.fileEndpoint(
                    KASSEN_IK, "Probe", tempDir.resolve("probe"))).reachable());
        }
    }
}
