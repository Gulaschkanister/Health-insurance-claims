package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.gkvtransmitter.dta.Absender;
import de.gkvtransmitter.dta.DtaFactory;
import de.gkvtransmitter.dta.Uebermittlungsart;
import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.entity.Patient;
import de.gkvtransmitter.entity.Protokolleintrag;
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
    /** Woher die absendende Stelle kommt, siehe {@link Betriebsangaben}. */
    private final Betriebsangaben betriebsangaben;
    /** Wohin das Uebermittlungsprotokoll geht, siehe {@link Protokollfuehrung}. */
    private final Protokollfuehrung protokoll;
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

    /**
     * Woher die Angaben zur absendenden Stelle kommen.
     *
     * <p>Als Abfrage und nicht als fester Wert im Konstruktor: Wer die
     * Betriebsdaten in der Maske aendert und danach abrechnet, soll die
     * geaenderten benutzen und nicht die vom Programmstart.</p>
     */
    @FunctionalInterface
    public interface Betriebsangaben {
        /** Die erfassten Betriebsdaten, oder {@code null}, wenn es keine gibt. */
        Betriebsdaten aktuelle();
    }

    /**
     * Wohin das Uebermittlungsprotokoll geschrieben wird.
     *
     * <p>Pflicht nach Anlage 1, Abschnitt 3 Absatz 2. Wie bei
     * {@link Datenaustauschreferenzen} als einmethodige Schnittstelle und
     * nicht als {@code DataRepository}: Der Dienst schreibt Eintraege und
     * liest keine, und ohne das ganze Repository bleibt er im Test ohne
     * Datenbank aufsetzbar.</p>
     */
    @FunctionalInterface
    public interface Protokollfuehrung {
        /** Nimmt einen Eintrag auf. */
        void vermerke(Protokolleintrag eintrag);
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
        this(endpointRegistry, transport, validierung, referenzen, art, () -> null);
    }

    /**
     * @param betriebsangaben woher die absendende Stelle kommt; liefert
     *        {@code null}, solange keine Betriebsdaten erfasst sind
     */
    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport,
            DtaValidationService validierung, Datenaustauschreferenzen referenzen, Uebermittlungsart art,
            Betriebsangaben betriebsangaben) {
        this(endpointRegistry, transport, validierung, referenzen, art, betriebsangaben, eintrag -> { });
    }

    /**
     * @param protokoll wohin die Pflichtdokumentation geht; ohne Angabe wird
     *        nichts festgehalten - fuer Tests, nicht fuer den Betrieb
     */
    public DtaDispatchService(BillingOfficeEndpointRegistry endpointRegistry, BillingOfficeTransport transport,
            DtaValidationService validierung, Datenaustauschreferenzen referenzen, Uebermittlungsart art,
            Betriebsangaben betriebsangaben, Protokollfuehrung protokoll) {
        this.protokoll = Objects.requireNonNull(protokoll, "protokoll must not be null");
        this.endpointRegistry = Objects.requireNonNull(endpointRegistry, "endpointRegistry must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.validierung = Objects.requireNonNull(validierung, "validierung must not be null");
        this.referenzen = Objects.requireNonNull(referenzen, "referenzen must not be null");
        this.art = Objects.requireNonNull(art, "art must not be null");
        this.betriebsangaben = Objects.requireNonNull(betriebsangaben, "betriebsangaben must not be null");
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
    private record ErzeugteNachricht(Abrechnung abrechnung, String inhalt, String dateiname,
            long referenz, String absenderIk, String empfaengerIk) {

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
     * <p>Der Rueckgabewert traegt <b>beides</b>: die Lieferungen und den
     * Pruefbericht. Der Bericht entsteht ohnehin und wurde frueher bei
     * fehlerfreiem Lauf verworfen - siehe {@link Versandergebnis}.</p>
     *
     * @throws DtaValidierungsException wenn eine der Nachrichten beanstandet
     *                                  wird - in dem Fall wurde nichts versendet
     */
    public Versandergebnis generateAndRoute(List<Abrechnung> abrechnungen, Path outDir) {
        Objects.requireNonNull(abrechnungen, "abrechnungen must not be null");
        Objects.requireNonNull(outDir, "outDir must not be null");

        List<ErzeugteNachricht> nachrichten = erzeuge(abrechnungen);

        // Erst pruefen, dann zustellen. Eine einzige beanstandete Nachricht
        // haelt den gesamten Lauf auf.
        ValidationReport bericht = betriebsdatenHinweis()
                .plus(unbekannteEmpfaenger(nachrichten))
                .plus(abweichendeAbrechnungscodes(abrechnungen));
        for (ErzeugteNachricht nachricht : nachrichten) {
            bericht = bericht.plus(validierung.pruefe(nachricht.inhalt()));
        }
        if (bericht.hatFehler()) {
            throw new DtaValidierungsException(bericht);
        }

        // Der Bericht wird mitgegeben, auch wenn er leer ist: was hier
        // uebrigbleibt, sind Warnungen und Hinweise, und die sollen die
        // Anwenderin erreichen.
        return new Versandergebnis(stelleZu(nachrichten, outDir), bericht);
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
                    leistungsparameter(abrechnung), art, absender(senderIk));
            String filename = String.format("patient_%d_%s.dta",
                    patient.getId(), LocalDateTime.now().format(FILE_TIME));

            erzeugt.add(new ErzeugteNachricht(abrechnung, content, filename, sequence,
                    absender(senderIk).ik(), receiverIk));
        }
        return erzeugt;
    }

    /**
     * Die Leistungsangaben einer Abrechnung.
     *
     * <p>Aus der Blaupause - bis auf den <b>Abrechnungscode</b>, den der
     * Leistungserbringer vorgibt, sobald einer an ihm hinterlegt ist. Der Code
     * gehoert zum Beruf und nicht zur Kursart; siehe
     * {@link de.gkvtransmitter.dta.Leistungsparameter#mitAbrechnungscode}.</p>
     */
    private static de.gkvtransmitter.dta.Leistungsparameter leistungsparameter(Abrechnung abrechnung) {
        de.gkvtransmitter.dta.Leistungsparameter ausBlaupause =
                de.gkvtransmitter.dta.Leistungsparameter.ausBlueprint(abrechnung.getBlueprint());
        de.gkvtransmitter.entity.ServiceProvider erbringer = abrechnung.getProvider();
        if (erbringer == null || !erbringer.hatAbrechnungscode()) {
            return ausBlaupause;
        }
        return ausBlaupause.mitAbrechnungscode(erbringer.getAbrechnungscode());
    }

    /**
     * Meldet, wenn Profil und Blaupause verschiedene Abrechnungscodes nennen.
     *
     * <p>Das Profil gewinnt - aber <b>still gewinnen darf es nicht</b>. Aus dem
     * Code ergibt sich der Leistungsbereich im {@code UNB}, und ein falscher
     * kommt aus Pruefstufe 3 als Zurueckweisung zurueck. Wer eine Blaupause
     * gepflegt hat und etwas anderes gesendet bekommt, soll das erfahren,
     * bevor es die Kasse tut.</p>
     *
     * <p>Ein Hinweis und keine Warnung: Die Lage ist nicht falsch, sie ist nur
     * nicht selbstverstaendlich. Genau dafuer gibt es INFO.</p>
     */
    private ValidationReport abweichendeAbrechnungscodes(List<Abrechnung> abrechnungen) {
        ValidationReport.Builder bericht = ValidationReport.builder();
        boolean etwasGemeldet = false;
        for (String paar : abrechnungen.stream().map(DtaDispatchService::codeabweichung)
                .filter(Objects::nonNull).distinct().toList()) {
            String[] teile = paar.split(" ");
            bericht.info("ABRECHNUNGSCODE_AUS_PROFIL", "ENF",
                    "Die Blaupause nennt den Abrechnungscode " + teile[1] + ", der Dienstleister "
                            + teile[0] + ". Gesendet wird " + teile[0] + ", weil der Code zum Beruf"
                            + " gehoert und nicht zur Kursart. Stimmt das nicht, gehoert er im"
                            + " Dienstleisterprofil geaendert.");
            etwasGemeldet = true;
        }
        return etwasGemeldet ? bericht.build() : ValidationReport.leer();
    }

    /** "Profilcode Blaupausencode", oder {@code null}, wenn beide gleich sind. */
    private static String codeabweichung(Abrechnung abrechnung) {
        de.gkvtransmitter.entity.ServiceProvider erbringer = abrechnung.getProvider();
        if (erbringer == null || !erbringer.hatAbrechnungscode()) {
            return null;
        }
        String ausProfil = erbringer.getAbrechnungscode().trim();
        String ausBlaupause = de.gkvtransmitter.dta.Leistungsparameter
                .ausBlueprint(abrechnung.getBlueprint()).abrechnungscode();
        return ausProfil.equals(ausBlaupause) ? null : ausProfil + " " + ausBlaupause;
    }

    /**
     * Die absendende Stelle fuer eine Nachricht.
     *
     * <p>Aus den Betriebsdaten, solange sie ein IK fuehren - sonst aus dem
     * Leistungserbringer, wie es bis zum 17.09.2026 immer war. Die
     * Rueckfallebene ist fuer eine allein arbeitende Hebamme richtig; dass sie
     * benutzt wurde, meldet {@link #betriebsdatenHinweis()} trotzdem.</p>
     */
    private Absender absender(String leistungserbringerIk) {
        Betriebsdaten betrieb = betriebsangaben.aktuelle();
        if (betrieb == null || !betrieb.sindVersandtauglich()) {
            return Absender.ausLeistungserbringer(leistungserbringerIk);
        }
        return new Absender(betrieb.getIk().trim(), betrieb.istSelbstabrechner());
    }

    /**
     * Ein Hinweis, solange die absendende Stelle nicht erfasst ist.
     *
     * <p>Bewusst kein Fehler: Eine Sperre haette jeden Lauf angehalten, bevor
     * die Maske ueberhaupt einmal geoeffnet werden konnte - und fuer den
     * haeufigsten Fall, eine Hebamme, die fuer sich selbst abrechnet, ist die
     * Rueckfallebene richtig. Gesehen werden soll es aber, denn fuer jeden
     * anderen Fall ist der Absender falsch.</p>
     */
    /**
     * Meldet jede Kasse, fuer die kein Ziel hinterlegt ist.
     *
     * <p>Die Zustellung weicht dann auf einen Sammelordner aus und meldet
     * Erfolg - die Datei liegt aber nur da. Solange die Zuordnung von Hand in
     * {@code billing-office-endpoints.json} gepflegt wird, ist diese Luecke der
     * Normalfall und nicht die Ausnahme; sie gehoert deshalb auf den
     * Bildschirm. Die dauerhafte Loesung liest die Zuordnung aus der
     * Kostentraegerdatei, siehe {@link Kostentraegerdatei}.</p>
     */
    private ValidationReport unbekannteEmpfaenger(List<ErzeugteNachricht> nachrichten) {
        ValidationReport.Builder bericht = ValidationReport.builder();
        boolean etwasGemeldet = false;
        for (int kassenIk : nachrichten.stream().map(ErzeugteNachricht::kassenIk).distinct().toList()) {
            if (!endpointRegistry.kenntKasse(kassenIk)) {
                bericht.warning("ANNAHMESTELLE_UNBEKANNT", "Kasse " + kassenIk,
                        "Fuer die Kasse " + kassenIk + " ist kein Ziel hinterlegt. Die Datei landet"
                                + " im Sammelordner und ist damit nicht zugestellt. Zustaendig ist"
                                + " die Datenannahmestelle mit Entschluesselungsbefugnis aus der"
                                + " Kostentraegerdatei.");
                etwasGemeldet = true;
            }
        }
        return etwasGemeldet ? bericht.build() : ValidationReport.leer();
    }

    private ValidationReport betriebsdatenHinweis() {
        Betriebsdaten betrieb = betriebsangaben.aktuelle();
        if (betrieb != null && betrieb.sindVersandtauglich()) {
            return ValidationReport.leer();
        }
        return ValidationReport.builder()
                .warning("BETRIEBSDATEN_FEHLEN", "UNB",
                        "Es sind keine Betriebsdaten erfasst. Als Absender steht deshalb das IK des"
                                + " Leistungserbringers in der Datei. Das stimmt, solange die"
                                + " Leistungserbringerin selbst abrechnet - sonst gehoert unter"
                                + " \"Betriebsdaten\" das eigene Institutionskennzeichen hinterlegt.")
                .build();
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
                OffsetDateTime beginn = OffsetDateTime.now();
                Path stagedFile = DtaFactory.writeDtaFile(nachricht.inhalt(), stagingFolder, nachricht.dateiname());
                Path deliveredFile = transport.send(stagedFile, endpoint);
                filesByKassenIk.computeIfAbsent(kassenIk, key -> new ArrayList<>()).add(deliveredFile);
                vermerke(nachricht, stagedFile, deliveredFile, beginn, null);
            } catch (IOException e) {
                // Auch der Fehlschlag gehoert ins Protokoll: Anhang 1,
                // Abschnitt 4.5 verlangt "fehlerfrei/fehlerhaft" und im
                // Fehlerfall den Status. Ein Protokoll, das nur die
                // gelungenen Faelle kennt, belegt nichts.
                vermerke(nachricht, null, null, OffsetDateTime.now(), e.getMessage());
                throw new DispatchException("Zustellung an Kasse " + kassenIk + " fehlgeschlagen", e);
            }
        }

        List<DispatchBatch> batches = new ArrayList<>();
        for (Map.Entry<Integer, List<Path>> entry : filesByKassenIk.entrySet()) {
            batches.add(new DispatchBatch(entry.getKey(), entry.getValue()));
        }
        return batches;
    }

    /**
     * Schreibt einen Eintrag ins Uebermittlungsprotokoll.
     *
     * <p>Die Felder folgen Anhang 1, Abschnitt 4.5 - siehe
     * {@link Protokolleintrag}. Die Sicherungskopie ist die Datei unter
     * {@code staging/}: Sie ist nach Abschnitt 3 Absatz 4 bis zur Bezahlung
     * vorzuhalten, und ohne den Vermerk wuesste niemand, welche das ist.</p>
     */
    private void vermerke(ErzeugteNachricht nachricht, Path kopie, Path zugestellt,
            OffsetDateTime beginn, String fehler) {
        Protokolleintrag eintrag = new Protokolleintrag();
        eintrag.setDateiname(zugestellt != null
                ? zugestellt.getFileName().toString() : nachricht.dateiname());
        eintrag.setErstelltAm(OffsetDateTime.now());
        eintrag.setLaufendeNummer(nachricht.referenz());
        eintrag.setAbsenderIk(nachricht.absenderIk());
        eintrag.setEmpfaengerIk(nachricht.empfaengerIk());
        eintrag.setBeginn(beginn);
        eintrag.setEnde(OffsetDateTime.now());
        eintrag.setGroesseBytes(groesse(zugestellt != null ? zugestellt : kopie));
        eintrag.setHinweise("Uebermittlungsart " + art.name().toLowerCase(java.util.Locale.GERMAN));
        eintrag.setRichtung(Protokolleintrag.Richtung.SENDEN);
        eintrag.setFehlerfrei(fehler == null);
        eintrag.setFehlerstatus(fehler);
        eintrag.setSicherungskopie(kopie == null ? null : kopie.toString());
        protokoll.vermerke(eintrag);
    }

    /** Die Dateigroesse, oder 0, wenn sich die Datei nicht befragen laesst. */
    private static long groesse(Path datei) {
        if (datei == null) {
            return 0L;
        }
        try {
            return Files.size(datei);
        } catch (IOException nichtLesbar) {
            return 0L;
        }
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
