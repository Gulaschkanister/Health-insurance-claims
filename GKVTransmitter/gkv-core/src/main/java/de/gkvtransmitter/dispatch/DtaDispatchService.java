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
import java.util.Objects;

import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Erzeugt die DTA-Dateien, prueft sie und stellt sie der jeweiligen Kasse zu.
 *
 * <p>Die Pruefung ist als Tor davorgeschaltet: erst werden alle Nachrichten
 * erzeugt und geprueft, und nur wenn keine davon beanstandet wird, geht
 * ueberhaupt eine hinaus. Andernfalls waere bei einem Fehler in der Mitte eines
 * Laufs ein Teil bereits zugestellt und muesste bei der Kasse storniert
 * werden.</p>
 */
public class DtaDispatchService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BillingOfficeEndpointRegistry endpointRegistry;
    private final BillingOfficeTransport transport;
    private final DtaValidationService validierung;
    private final BillingOfficeResponseParser antwortAuswertung = new BillingOfficeResponseParser();

    public DtaDispatchService() {
        this(BillingOfficeEndpointRegistry.loadDefault(), new FileBillingOfficeTransport());
    }

    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport) {
        this(endpointRegistry, transport, DtaValidationService.standard());
    }

    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport,
            DtaValidationService validierung) {
        this.endpointRegistry = Objects.requireNonNull(endpointRegistry, "endpointRegistry must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.validierung = Objects.requireNonNull(validierung, "validierung must not be null");
    }

    /**
     * Eine erzeugte, noch nicht zugestellte Nachricht.
     *
     * @param abrechnung die zugrunde liegende Abrechnung
     * @param inhalt     die erzeugte DTA-Nachricht
     * @param dateiname  der vorgesehene Dateiname
     */
    private record ErzeugteNachricht(Abrechnung abrechnung, String inhalt, String dateiname) {

        int kassenIk() {
            return abrechnung.getPatient().getKassenIk();
        }
    }

    /**
     * Prueft, was fuer die Abrechnungen erzeugt wuerde, ohne etwas zu
     * versenden.
     *
     * <p>Der Pruefeinstieg fuer die Oberflaeche: die Anwenderin kann eine
     * Abrechnung kontrollieren, bevor sie sie herausgibt.</p>
     */
    public ValidationReport pruefe(List<Abrechnung> abrechnungen) {
        Objects.requireNonNull(abrechnungen, "abrechnungen must not be null");
        ValidationReport gesamt = ValidationReport.leer();
        for (ErzeugteNachricht nachricht : erzeuge(abrechnungen)) {
            gesamt = gesamt.plus(validierung.pruefe(nachricht.inhalt()));
        }
        return gesamt;
    }

    /** Prueft eine einzelne, bereits vorliegende DTA-Nachricht. */
    public ValidationReport pruefeNachricht(String dtaInhalt) {
        return validierung.pruefe(dtaInhalt);
    }

    /**
     * Erzeugt, prueft und verteilt die Abrechnungen.
     *
     * @throws DtaValidierungsException wenn eine der Nachrichten beanstandet
     *                                  wird - in dem Fall wurde nichts versendet
     */
    public List<DispatchBatch> generateAndRoute(List<Abrechnung> abrechnungen, Path outDir) {
        Objects.requireNonNull(abrechnungen, "abrechnungen must not be null");
        Objects.requireNonNull(outDir, "outDir must not be null");

        List<ErzeugteNachricht> nachrichten = erzeuge(abrechnungen);

        // Erst pruefen, dann zustellen. Eine einzige beanstandete Nachricht
        // haelt den gesamten Lauf auf.
        ValidationReport bericht = ValidationReport.leer();
        for (ErzeugteNachricht nachricht : nachrichten) {
            bericht = bericht.plus(validierung.pruefe(nachricht.inhalt()));
        }
        if (bericht.hatFehler()) {
            throw new DtaValidierungsException(bericht);
        }

        return stelleZu(nachrichten, outDir);
    }

    private List<ErzeugteNachricht> erzeuge(List<Abrechnung> abrechnungen) {
        List<ErzeugteNachricht> erzeugt = new ArrayList<>();
        int sequence = 1;

        for (Abrechnung abrechnung : abrechnungen) {
            Patient patient = abrechnung.getPatient();
            String senderIk = String.valueOf(abrechnung.getProvider().getIk());
            String receiverIk = String.valueOf(patient.getKassenIk());
            String content = DtaFactory.buildDtaFor(abrechnung, sequence, senderIk, receiverIk);
            String filename = String.format("patient_%d_%s.dta",
                    patient.getId(), LocalDateTime.now().format(FILE_TIME));

            erzeugt.add(new ErzeugteNachricht(abrechnung, content, filename));
            sequence++;
        }
        return erzeugt;
    }

    private List<DispatchBatch> stelleZu(List<ErzeugteNachricht> nachrichten, Path outDir) {
        Map<Integer, List<Path>> filesByKassenIk = new LinkedHashMap<>();

        for (ErzeugteNachricht nachricht : nachrichten) {
            int kassenIk = nachricht.kassenIk();
            BillingOfficeEndpoint endpoint = endpointRegistry.resolve(kassenIk, outDir.resolve("outbox"));
            Path stagingFolder = outDir.resolve("staging").resolve(String.valueOf(kassenIk));
            try {
                if (!Files.exists(stagingFolder)) {
                    Files.createDirectories(stagingFolder);
                }
                Path stagedFile = DtaFactory.writeDtaFile(nachricht.inhalt(), stagingFolder, nachricht.dateiname());
                Path deliveredFile = transport.send(stagedFile, endpoint);
                filesByKassenIk.computeIfAbsent(kassenIk, key -> new ArrayList<>()).add(deliveredFile);
            } catch (IOException e) {
                throw new DispatchException("Zustellung an Kasse " + kassenIk + " fehlgeschlagen", e);
            }
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

    /**
     * Wertet die Rueckmeldung einer Kasse aus.
     *
     * <p>Die Auswertung selbst liegt in {@link BillingOfficeResponseParser} -
     * sie stand vorher hier und hatte dabei zwei Fehler, die eine abgelehnte
     * Lieferung als angenommen erscheinen lassen konnten.</p>
     */
    public BillingOfficeResponse parseResponse(String rawContent) {
        return antwortAuswertung.parse(rawContent);
    }
}
