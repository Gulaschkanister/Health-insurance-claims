package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.model.Abrechnung;

public class DtaDispatchService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BillingOfficeEndpointRegistry endpointRegistry;
    private final BillingOfficeTransport transport;

    public DtaDispatchService() {
        this(BillingOfficeEndpointRegistry.loadDefault(), new FileBillingOfficeTransport());
    }

    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport) {
        this.endpointRegistry = endpointRegistry;
        this.transport = transport;
    }

    public List<DispatchBatch> generateAndRoute(List<Abrechnung> abrechnungen, Path outDir) {
        Map<Integer, List<Path>> filesByKassenIk = new LinkedHashMap<>();
        int sequence = 1;

        for (Abrechnung abrechnung : abrechnungen) {
            Patient patient = abrechnung.getPatient();
            int kassenIk = patient.getKassenIk();
            String senderIk = String.valueOf(abrechnung.getProvider().getIk());
            String receiverIk = String.valueOf(patient.getKassenIk());
            String content = DtaFactory.buildDtaFor(abrechnung, sequence, senderIk, receiverIk);
            String filename = String.format("patient_%d_%s.dta", patient.getId(), LocalDateTime.now().format(FILE_TIME));

            BillingOfficeEndpoint endpoint = endpointRegistry.resolve(kassenIk, outDir.resolve("outbox"));
            Path stagingFolder = outDir.resolve("staging").resolve(String.valueOf(kassenIk));
            try {
                if (!Files.exists(stagingFolder)) {
                    Files.createDirectories(stagingFolder);
                }
                Path stagedFile = DtaFactory.writeDtaFile(content, stagingFolder, filename);
                Path deliveredFile = transport.send(stagedFile, endpoint);
                filesByKassenIk.computeIfAbsent(kassenIk, key -> new ArrayList<>()).add(deliveredFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not dispatch DTA file for kassenIk=" + kassenIk, e);
            }
            sequence++;
        }

        List<DispatchBatch> batches = new ArrayList<>();
        for (Map.Entry<Integer, List<Path>> entry : filesByKassenIk.entrySet()) {
            batches.add(new DispatchBatch(entry.getKey(), entry.getValue()));
        }
        return batches;
    }

    public List<BillingOfficeEndpointCheck> checkConfiguredEndpoints() {
        List<BillingOfficeEndpointCheck> checks = new ArrayList<>();
        for (BillingOfficeEndpoint endpoint : endpointRegistry.getEndpoints().values()) {
            checks.add(transport.probe(endpoint));
        }
        return checks;
    }

    public BillingOfficeResponse parseResponse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return new BillingOfficeResponse(BillingOfficeResponseType.UNKNOWN, "Leere Antwort", rawContent);
        }

        String normalized = rawContent.toLowerCase();
        if (normalized.contains("syntaxfehler") || normalized.contains("ungültige länge")) {
            return new BillingOfficeResponse(BillingOfficeResponseType.SYNTAX_ERROR, "Syntaxfehler", rawContent);
        }
        if (normalized.contains("abgelehnt") || normalized.contains("zurückgewiesen") || normalized.contains("fehler")) {
            return new BillingOfficeResponse(BillingOfficeResponseType.REJECTED, "Abgelehnt", rawContent);
        }
        if (normalized.contains("angenommen") || normalized.contains("erfolgreich") || normalized.contains("ok")) {
            return new BillingOfficeResponse(BillingOfficeResponseType.ACCEPTED, "Angenommen", rawContent);
        }
        if (normalized.contains("technisch") || normalized.contains("timeout") || normalized.contains("nicht erreichbar")) {
            return new BillingOfficeResponse(BillingOfficeResponseType.TECHNICAL_ERROR, "Technischer Fehler", rawContent);
        }

        return new BillingOfficeResponse(BillingOfficeResponseType.UNKNOWN, "Unbekannte Antwort", rawContent);
    }
}