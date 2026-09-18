package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import de.gkvtransmitter.dta.Uebermittlungsart;
import de.gkvtransmitter.entity.Betriebsdaten;
import de.gkvtransmitter.model.Abrechnung;
import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Einmal durchspielen, ohne zu senden.
 *
 * <p>{@link SimulierteKassenGegenstelle} und {@link SimulierterKassenTransport}
 * gibt es seit Langem - <b>erreichbar waren sie nur aus Tests.</b> Wer vor dem
 * ersten Echtversand wissen wollte, ob eine Lieferung durchkommt, musste einen
 * Test schreiben. Das ist die Uebung, die sich am meisten lohnt und bisher am
 * wenigsten zu haben war.</p>
 *
 * <h2>Ein Trockenlauf darf nichts hinterlassen</h2>
 *
 * <p>Das ist die eigentliche Schwierigkeit, und drei Dinge gehoeren dazu:</p>
 *
 * <ol>
 *   <li><b>Kein Protokolleintrag.</b> Das Uebermittlungsprotokoll ist eine
 *       Pflichtdokumentation nach Anlage 1, Abschnitt 3 Absatz 2 und zwei Jahre
 *       aufzubewahren. Eine Uebung darin waere eine Falschaussage.</li>
 *   <li><b>Keine verbrauchte Datenaustauschreferenz.</b> Jede darf nur einmal
 *       vergeben werden; wer sie im Trockenlauf verbraucht, hinterlaesst
 *       Luecken in der echten Folge. Hier zaehlt deshalb ein eigener Zaehler
 *       bei 1 los.</li>
 *   <li><b>Keine Datei im Versandordner.</b> Alles landet unter einem
 *       Verzeichnis in {@code java.io.tmpdir}, das der Aufrufer ansehen und
 *       wegwerfen kann.</li>
 * </ol>
 *
 * <p>Die <b>Uebermittlungsart</b> ist fest {@link Uebermittlungsart#TEST} und
 * nicht die eingestellte. Sollte eine dieser Dateien je irgendwo landen, soll
 * sie sich selbst als Test ausweisen - dieselbe Ueberlegung wie bei der
 * Rueckfallebene, die nie auf {@code ECHT} faellt.</p>
 */
public final class Trockenlauf {

    /** Vorsatz des Verzeichnisses, in dem ein Trockenlauf seine Dateien ablegt. */
    private static final String ORDNERVORSATZ = "gkv-trockenlauf-";

    /**
     * Was ein Trockenlauf ergeben hat.
     *
     * @param bericht    der Pruefbericht, auch wenn er zum Abbruch fuehrte
     * @param bestanden  ob die Pruefung bestanden wurde
     * @param antworten  was die simulierte Gegenstelle je Kasse zurueckmeldet
     * @param dateien    wie viele Dateien entstanden waeren
     * @param ordner     wo sie liegen, zum Ansehen
     */
    public record Ergebnis(ValidationReport bericht, boolean bestanden,
            Map<Integer, BillingOfficeResponse> antworten, int dateien, Path ordner) {

        /** Ob es ueberhaupt etwas zu melden gibt. */
        public boolean hatBefunde() {
            return !bericht.istLeer();
        }

        /** Ob die simulierte Kasse jede Lieferung angenommen haette. */
        public boolean alleAngenommen() {
            return !antworten.isEmpty() && antworten.values().stream()
                    .allMatch(antwort -> antwort.getType() == BillingOfficeResponseType.ACCEPTED);
        }
    }

    private final DtaDispatchService.Betriebsangaben betriebsangaben;
    private final DtaValidationService validierung;

    /**
     * @param betriebsangaben dieselben wie im Echtbetrieb - sonst pruefte der
     *        Trockenlauf eine Datei, die es so nie gaebe
     */
    public Trockenlauf(DtaDispatchService.Betriebsangaben betriebsangaben) {
        this(betriebsangaben, DtaValidationService.standard());
    }

    public Trockenlauf(DtaDispatchService.Betriebsangaben betriebsangaben,
            DtaValidationService validierung) {
        this.betriebsangaben = Objects.requireNonNull(betriebsangaben, "betriebsangaben must not be null");
        this.validierung = Objects.requireNonNull(validierung, "validierung must not be null");
    }

    /** Mit den Betriebsdaten aus der Datenbank. */
    public static Trockenlauf mitBetriebsdaten(java.util.function.Supplier<Betriebsdaten> quelle) {
        return new Trockenlauf(quelle::get);
    }

    /**
     * Spielt die Abrechnungen durch.
     *
     * <p>Wirft nicht: Eine nicht bestandene Pruefung ist hier kein Fehlschlag,
     * sondern <b>das Ergebnis</b>. Genau dafuer spielt man durch.</p>
     */
    public Ergebnis spiele(List<Abrechnung> abrechnungen) {
        Objects.requireNonNull(abrechnungen, "abrechnungen must not be null");
        Path ordner = arbeitsordner();
        SimulierterKassenTransport transport = new SimulierterKassenTransport(
                new FileBillingOfficeTransport(), new SimulierteKassenGegenstelle("Trockenlauf"));

        // Alles in den eigenen Ordner: Die hinterlegten Ziele bleiben
        // unberuehrt, sonst laege eine Uebungsdatei in der echten Ausgabe.
        DtaDispatchService dienst = new DtaDispatchService(
                new BillingOfficeEndpointRegistry(new LinkedHashMap<>(), ordner.resolve("eingang")),
                transport, validierung, new AtomicLong(1)::getAndIncrement,
                Uebermittlungsart.TEST, betriebsangaben,
                // Kein Protokolleintrag - siehe Klassenkommentar.
                eintrag -> { });

        try {
            Versandergebnis ergebnis = dienst.generateAndRoute(abrechnungen, ordner);
            int dateien = ergebnis.lieferungen().stream()
                    .mapToInt(lieferung -> lieferung.getFiles().size()).sum();
            return new Ergebnis(ergebnis.bericht(), true, transport.alleAntworten(), dateien, ordner);
        } catch (DtaValidierungsException beanstandet) {
            return new Ergebnis(beanstandet.getBericht(), false, Map.of(), 0, ordner);
        } catch (DispatchException nichtZugestellt) {
            // Auch das ist ein Ergebnis: Im Trockenlauf kann nur der eigene
            // Ordner klemmen, und das soll man sehen statt als Absturz.
            return new Ergebnis(ValidationReport.builder()
                    .fehler("TROCKENLAUF_ORDNER", ordner.toString(), nichtZugestellt.getMessage())
                    .build(), false, Map.of(), 0, ordner);
        }
    }

    /** Ein frisches Verzeichnis je Lauf; zwei Laeufe sollen sich nicht mischen. */
    private static Path arbeitsordner() {
        try {
            return Files.createTempDirectory(ORDNERVORSATZ);
        } catch (IOException keinTempVerzeichnis) {
            throw new IllegalStateException(
                    "Kein Verzeichnis fuer den Trockenlauf: " + keinTempVerzeichnis.getMessage(),
                    keinTempVerzeichnis);
        }
    }

    /** Die Antworten in fester Reihenfolge, fuer die Anzeige. */
    public static List<Map.Entry<Integer, BillingOfficeResponse>> geordnet(
            Map<Integer, BillingOfficeResponse> antworten) {
        List<Map.Entry<Integer, BillingOfficeResponse>> geordnet = new ArrayList<>(antworten.entrySet());
        geordnet.sort(Map.Entry.comparingByKey());
        return geordnet;
    }
}
