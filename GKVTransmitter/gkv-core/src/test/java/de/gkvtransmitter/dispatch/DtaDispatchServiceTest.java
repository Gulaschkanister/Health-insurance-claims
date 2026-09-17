package de.gkvtransmitter.dispatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.gkvtransmitter.dta.Testblaupause;
import de.gkvtransmitter.entity.Blueprint;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.ServiceProvider;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.DtaValidationService;

class DtaDispatchServiceTest {

    @TempDir
    Path tempDir;

    /**
     * Der Pruefbericht verlaesst den Dienst auch dann, wenn nichts zu
     * beanstanden war.
     *
     * <p>Bis zum 17.09.2026 lieferte {@code generateAndRoute} nur die
     * Lieferungen. Der Bericht entstand, wurde geprueft und danach fallen
     * gelassen — Warnungen und Hinweise konnten die Oberflaeche also gar nicht
     * erreichen, weil nur die Ausnahme einen Bericht mitbrachte. Dieser Test
     * haelt fest, dass der Rueckgabewert beides traegt.</p>
     */
    @Test
    @DisplayName("Ein gelungener Lauf gibt Lieferungen und Bericht zurueck")
    void ergebnisTraegtBeides() throws Exception {
        Map<Integer, BillingOfficeEndpoint> endpunkte = new LinkedHashMap<>();
        endpunkte.put(108310400,
                BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse", tempDir.resolve("ziel")));
        DtaDispatchService dienst = new DtaDispatchService(
                new BillingOfficeEndpointRegistry(endpunkte, tempDir.resolve("fallback")),
                new FileBillingOfficeTransport());

        Versandergebnis ergebnis = dienst.generateAndRoute(zweiAbrechnungen(), tempDir);

        assertFalse(ergebnis.lieferungen().isEmpty(), "Es sollte zugestellt worden sein");
        assertNotNull(ergebnis.bericht(), "Der Bericht gehoert zum Ergebnis, auch wenn er leer ist");
        // Ein Fehler haette den Lauf beendet; was hier ankommt, sind hoechstens
        // Warnungen.
        assertFalse(ergebnis.bericht().hatFehler(),
                "Ein Ergebnis mit Fehlern darf es nicht geben - das waere eine Ausnahme gewesen");
    }

    /**
     * Der Absender im UNB kommt aus den Betriebsdaten, nicht aus dem
     * Dienstleister.
     *
     * <p>Bis zum 17.09.2026 stand im UNB immer das IK des
     * Leistungserbringers, und die neunte Stelle des logischen Dateinamens
     * fest auf {@code S}. Fuer eine allein arbeitende Hebamme stimmte das;
     * sobald eine Abrechnungsstelle absendet, nicht mehr.</p>
     */
    @Test
    @DisplayName("Absender und Dateiname kommen aus den Betriebsdaten")
    void absenderAusBetriebsdaten() throws Exception {
        de.gkvtransmitter.entity.Betriebsdaten betrieb = new de.gkvtransmitter.entity.Betriebsdaten();
        betrieb.setIk("101560000");
        betrieb.setSelbstabrechner(false);

        Map<Integer, BillingOfficeEndpoint> endpunkte = new LinkedHashMap<>();
        endpunkte.put(108310400,
                BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse", tempDir.resolve("ziel")));
        DtaDispatchService dienst = new DtaDispatchService(
                new BillingOfficeEndpointRegistry(endpunkte, tempDir.resolve("fallback")),
                new FileBillingOfficeTransport(), DtaValidationService.standard(),
                new DtaDispatchService.LaufenderZaehler(),
                de.gkvtransmitter.dta.Uebermittlungsart.ERPROBUNG,
                () -> betrieb);

        dienst.generateAndRoute(zweiAbrechnungen(), tempDir);

        String unb = ersteZeile(tempDir.resolve("ziel"));
        assertEquals("101560000", unb.split("\\+")[2],
                "Im UNB muss das IK des Betriebs stehen, nicht das des Leistungserbringers");
        // Stellen 3 bis 8 des Betriebs-IK, dann 'A' fuer Abrechnungsstelle.
        assertTrue(unb.contains("+SL156000A"), "Logischer Dateiname falsch gebildet: " + unb);
    }

    @Test
    @DisplayName("Ohne Betriebsdaten bleibt es beim Leistungserbringer - mit Hinweis")
    void ohneBetriebsdatenRueckfallUndHinweis() throws Exception {
        Map<Integer, BillingOfficeEndpoint> endpunkte = new LinkedHashMap<>();
        endpunkte.put(108310400,
                BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse", tempDir.resolve("ziel")));
        DtaDispatchService dienst = new DtaDispatchService(
                new BillingOfficeEndpointRegistry(endpunkte, tempDir.resolve("fallback")),
                new FileBillingOfficeTransport());

        Versandergebnis ergebnis = dienst.generateAndRoute(zweiAbrechnungen(), tempDir);

        assertEquals("104940005", ersteZeile(tempDir.resolve("ziel")).split("\\+")[2],
                "Ohne Betriebsdaten gilt weiterhin das IK des Leistungserbringers");
        assertTrue(ergebnis.bericht().getWarnings().stream()
                        .anyMatch(befund -> "BETRIEBSDATEN_FEHLEN".equals(befund.code())),
                "Die Rueckfallebene soll auffallen: " + ergebnis.bericht().alsText());
        assertTrue(ergebnis.bericht().istVersandfaehig(),
                "Ein Hinweis, kein Fehler - sonst waere es eine Sperre ohne Ausgang");
    }

    /**
     * Eine Kasse ohne hinterlegtes Ziel faellt auf.
     *
     * <p>Die Zustellung weicht dann auf einen Sammelordner aus und meldet
     * Erfolg. <b>Die Datei liegt aber nur da</b> - wer das nicht erfaehrt,
     * wartet auf eine Zahlung, die nie kommt.</p>
     */
    @Test
    @DisplayName("Eine Kasse ohne hinterlegtes Ziel wird gemeldet")
    void meldetUnbekanntenEmpfaenger() {
        DtaDispatchService dienst = new DtaDispatchService(
                new BillingOfficeEndpointRegistry(new LinkedHashMap<>(), tempDir.resolve("sammel")),
                new FileBillingOfficeTransport());

        Versandergebnis ergebnis = dienst.generateAndRoute(zweiAbrechnungen(), tempDir);

        assertTrue(ergebnis.bericht().getWarnings().stream()
                        .anyMatch(befund -> "ANNAHMESTELLE_UNBEKANNT".equals(befund.code())),
                "Der Sammelordner darf nicht stillschweigend einspringen: "
                        + ergebnis.bericht().alsText());
        assertTrue(ergebnis.bericht().istVersandfaehig(),
                "Ein Hinweis - die Datei ist ja erzeugt und abgelegt");
    }

    /** Die erste Zeile der zuerst gefundenen Datei im Ordner. */
    private static String ersteZeile(Path ordner) throws Exception {
        try (var dateien = Files.list(ordner)) {
            Path datei = dateien.sorted().findFirst()
                    .orElseThrow(() -> new AssertionError("Keine Datei in " + ordner));
            return Files.readString(datei).lines().findFirst()
                    .orElseThrow(() -> new AssertionError("Datei ist leer: " + datei));
        }
    }

    /**
     * Keine Datenaustauschreferenz zweimal — auch nicht ueber Laeufe hinweg.
     *
     * <p>Bis zum 06.09.2026 zaehlte {@code erzeuge} mit einer lokalen
     * Variablen, die bei jedem Lauf wieder bei 1 begann. Die zweite Abrechnung
     * eines Monats trug damit dieselben Referenzen wie die erste, und
     * <b>„Datenaustauschreferenz doppelt vergeben" ist ein dokumentierter
     * Abweisungsgrund</b>. Die gesperrte, gepruefte Methode dafuer
     * ({@code DataRepository.nextDtaInterchangeReference}) gab es die ganze
     * Zeit — sie hatte nur keinen Aufrufer.</p>
     *
     * <p>Geprueft wird am erzeugten UNB, nicht an einem Zaehlerstand: die
     * Referenz steht dort als fuenfstellige Zahl, und darauf sieht die
     * Kasse.</p>
     */
    @Test
    @DisplayName("Keine Datenaustauschreferenz wird zweimal vergeben")
    void referenzenSindEindeutig() throws Exception {
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(
                new LinkedHashMap<>(), tempDir.resolve("fallback"));
        // Eine dauerhafte Quelle, wie sie die Anwendung mitgibt.
        java.util.concurrent.atomic.AtomicLong dauerhaft = new java.util.concurrent.atomic.AtomicLong(1);

        Set<String> gesehen = new java.util.HashSet<>();
        for (int lauf = 0; lauf < 2; lauf++) {
            DtaDispatchService dienst = new DtaDispatchService(registry,
                    new FileBillingOfficeTransport(), DtaValidationService.standard(),
                    dauerhaft::getAndIncrement);
            for (DispatchBatch lieferung : dienst.generateAndRoute(zweiAbrechnungen(), tempDir).lieferungen()) {
                for (Path datei : lieferung.getFiles()) {
                    assertTrue(gesehen.add(referenzAus(Files.readString(datei))),
                            "Diese Referenz wurde schon einmal vergeben");
                }
            }
        }

        assertEquals(4, gesehen.size(), "Zwei Laeufe mit je zwei Abrechnungen");
    }

    /** Die Datenaustauschreferenz aus dem UNB: das fuenfte Element. */
    private static String referenzAus(String dta) {
        String unb = dta.lines().filter(zeile -> zeile.startsWith("UNB+")).findFirst()
                .orElseThrow(() -> new AssertionError("Kein UNB in der Nachricht"));
        return unb.split("\\+")[5];
    }

    private static List<Abrechnung> zweiAbrechnungen() {
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2",
                54321, 104940005, 101560000, LocalDate.of(1985, 2, 2));
        provider.setId(10);
        Blueprint blueprint = Testblaupause.mitPreis();
        Patient anna = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345,
                108310400, 108310400, LocalDate.of(1990, 1, 1));
        anna.setId(1);
        Patient ben = new Patient("Ben", "Beispiel", "Musterstrasse", "DE", "3", 12345,
                102137985, 104940005, LocalDate.of(1991, 2, 2));
        ben.setId(2);
        return List.of(new Abrechnung(anna, provider, blueprint, 1),
                new Abrechnung(ben, provider, blueprint, 2));
    }

    @Test
    void generatesFilesGroupedByKassenIk() throws Exception {
        ServiceProvider provider = new ServiceProvider("Max", "Muster", "Musterweg", "DE", "2", 54321, 104940005, 101560000, LocalDate.of(1985, 2, 2));
        provider.setId(10);
        Blueprint blueprint = Testblaupause.mitPreis();
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
        List<DispatchBatch> batches = service.generateAndRoute(abrechnungen, tempDir).lieferungen();

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
        Blueprint blueprint = Testblaupause.mitPreis();
        Patient patient = new Patient("Anna", "Beispiel", "Musterstrasse", "DE", "1", 12345, 108310400, 108310400, LocalDate.of(1990, 1, 1));
        patient.setId(1);

        List<Abrechnung> abrechnungen = List.of(new Abrechnung(patient, provider, blueprint, 1));

        Path endpointDir = tempDir.resolve("endpoint");
        Map<Integer, BillingOfficeEndpoint> endpoints = new LinkedHashMap<>();
        endpoints.put(108310400, BillingOfficeEndpoint.fileEndpoint(108310400, "Test-Kasse", endpointDir));
        BillingOfficeEndpointRegistry registry = new BillingOfficeEndpointRegistry(endpoints, tempDir.resolve("fallback"));

        DtaDispatchService service = new DtaDispatchService(registry, new FileBillingOfficeTransport());
        List<DispatchBatch> batches = service.generateAndRoute(abrechnungen, tempDir).lieferungen();

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
