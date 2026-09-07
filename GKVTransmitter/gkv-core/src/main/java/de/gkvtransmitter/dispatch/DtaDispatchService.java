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
import de.gkvtransmitter.dta.Uebermittlungsart;
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
    private final Datenaustauschreferenzen referenzen;
    /** Wofuer sich die erzeugten Dateien ausgeben, siehe {@link Uebermittlungsart}. */
    private final Uebermittlungsart art;
    private final BillingOfficeResponseParser antwortAuswertung = new BillingOfficeResponseParser();

    /**
     * Woher die Datenaustauschreferenzen kommen.
     *
     * <p>Als Schnittstelle und nicht als {@code DataRepository}: dieser Dienst
     * braucht von der Datenbank genau eine Zahl, und mit dem ganzen Repository
     * liesse er sich im Test nicht mehr ohne Weiteres aufsetzen. Dieselbe
     * Ueberlegung wie bei {@code Abrechnungslauf} in der Oberflaeche.</p>
     */
    @FunctionalInterface
    public interface Datenaustauschreferenzen {
        /** Die naechste Referenz; jede darf nur einmal vergeben werden. */
        long naechste();
    }

    public DtaDispatchService() {
        this(BillingOfficeEndpointRegistry.loadDefault(), new FileBillingOfficeTransport());
    }

    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport) {
        this(endpointRegistry, transport, DtaValidationService.standard());
    }

    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport,
            DtaValidationService validierung) {
        this(endpointRegistry, transport, validierung, new LaufenderZaehler());
    }

    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport,
            DtaValidationService validierung, Datenaustauschreferenzen referenzen) {
        this(endpointRegistry, transport, validierung, referenzen, Uebermittlungsart.ERPROBUNG);
    }

    /**
     * @param art wofuer sich die erzeugten Dateien ausgeben; siehe
     *        {@link Uebermittlungsart}
     */
    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport,
            DtaValidationService validierung, Datenaustauschreferenzen referenzen, Uebermittlungsart art) {
        this.endpointRegistry = Objects.requireNonNull(endpointRegistry, "endpointRegistry must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.validierung = Objects.requireNonNull(validierung, "validierung must not be null");
        this.referenzen = Objects.requireNonNull(referenzen, "referenzen must not be null");
        this.art = Objects.requireNonNull(art, "art must not be null");
    }

    /**
     * Die Rueckfallebene: zaehlt je Dienst hoch, beginnend bei 1.
     *
     * <p><b>Fuer einen echten Versand ist das zu wenig</b>, und bis zum
     * 06.09.2026 war es der einzige Weg: {@code erzeuge} zaehlte mit einer
     * lokalen Variablen, die bei jedem Lauf wieder bei 1 anfing. Die zweite
     * Abrechnung eines Monats trug damit dieselben Referenzen wie die erste,
     * und "Datenaustauschreferenz doppelt vergeben" ist ein dokumentierter
     * Abweisungsgrund.</p>
     *
     * <p>Ueber die Datenbank vergeben wurde sie nie - {@code DataRepository}
     * hatte die gesperrte, gepruefte Methode
     * {@code nextDtaInterchangeReference()} von Anfang an, und
     * <b>niemand rief sie auf</b>. Ein Test darauf gab es; einen Aufrufer
     * nicht.</p>
     *
     * <p>Diese Ebene bleibt fuer Tests und fuer den Fall, dass jemand den
     * Dienst ohne Datenbank benutzt. Wer echt versendet, gibt eine dauerhafte
     * Quelle mit - siehe {@code AbrechnungService}.</p>
     */
    static final class LaufenderZaehler implements Datenaustauschreferenzen {
        private final java.util.concurrent.atomic.AtomicLong stand =
                new java.util.concurrent.atomic.AtomicLong(1);

        @Override
        public long naechste() {
            return stand.getAndIncrement();
        }
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

        for (Abrechnung abrechnung : abrechnungen) {
            Patient patient = abrechnung.getPatient();
            String senderIk = String.valueOf(abrechnung.getProvider().getIk());
            String receiverIk = String.valueOf(patient.getKassenIk());
            // Jede Nachricht bekommt ihre eigene Referenz, und die kommt von
            // aussen - hier stand eine lokale Variable, die bei jedem Lauf
            // wieder bei 1 begann. Siehe LaufenderZaehler.
            long sequence = referenzen.naechste();
            String content = DtaFactory.buildDtaFor(abrechnung, sequence, senderIk, receiverIk,
                    de.gkvtransmitter.dta.Leistungsparameter.ausBlueprint(abrechnung.getBlueprint()), art);
            String filename = String.format("patient_%d_%s.dta",
                    patient.getId(), LocalDateTime.now().format(FILE_TIME));

            erzeugt.add(new ErzeugteNachricht(abrechnung, content, filename));
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
