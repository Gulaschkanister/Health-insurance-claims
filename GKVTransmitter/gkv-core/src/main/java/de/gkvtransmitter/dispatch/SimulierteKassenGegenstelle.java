package de.gkvtransmitter.dispatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.gkvtransmitter.validator.DtaValidationService;
import de.gkvtransmitter.validator.ValidationMessage;
import de.gkvtransmitter.validator.ValidationReport;

/**
 * Eine Gegenstelle, die sich wie eine Kasse verhaelt, ohne eine zu sein.
 *
 * <p>Der Datei-Transport legt eine Datei in einem Verzeichnis ab und gilt damit
 * als erledigt. Ob die Gegenseite die Lieferung annehmen wuerde, bleibt offen -
 * das zeigt sich sonst erst Tage spaeter im Fehlerprotokoll der Kasse.</p>
 *
 * <p>Diese Gegenstelle nimmt die Datei entgegen, prueft sie mit demselben
 * Regelwerk wie die Anwendung und schreibt ein Antwortprotokoll in der Form,
 * wie es eine Kasse zurueckmeldet. Damit laesst sich der gesamte Ablauf
 * einschliesslich Ablehnung und Fehlerprotokoll durchspielen, ohne echte
 * Zugangsdaten oder Zertifikate.</p>
 *
 * <p>Sie ersetzt keinen echten Versand. Was sie leistet, ist eine
 * nachvollziehbare Antwort auf eine konkrete Datei - und damit eine
 * Rueckmeldung schon vor dem ersten Echtversand.</p>
 */
public final class SimulierteKassenGegenstelle {

    private static final DateTimeFormatter PROTOKOLL_ZEIT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Endung der erzeugten Antwortprotokolle. */
    public static final String PROTOKOLL_ENDUNG = ".antwort.txt";

    private final DtaValidationService validierung;
    private final String kassenname;

    public SimulierteKassenGegenstelle(String kassenname) {
        this(kassenname, DtaValidationService.standard());
    }

    public SimulierteKassenGegenstelle(String kassenname, DtaValidationService validierung) {
        this.kassenname = Objects.requireNonNull(kassenname, "kassenname must not be null");
        this.validierung = Objects.requireNonNull(validierung, "validierung must not be null");
    }

    /**
     * Nimmt eine Lieferung entgegen und bewertet sie.
     *
     * @param dtaInhalt der Inhalt der uebermittelten Datei
     * @return die Rueckmeldung, wie sie eine Kasse formulieren wuerde
     */
    public BillingOfficeResponse empfange(String dtaInhalt) {
        if (dtaInhalt == null || dtaInhalt.isBlank()) {
            return new BillingOfficeResponse(BillingOfficeResponseType.SYNTAX_ERROR,
                    "Syntaxfehler", protokoll("Die Lieferung war leer.", List.of()));
        }

        ValidationReport bericht = validierung.pruefe(dtaInhalt);

        if (bericht.istVersandfaehig()) {
            return new BillingOfficeResponse(BillingOfficeResponseType.ACCEPTED, "Angenommen",
                    protokoll("Die Lieferung wurde angenommen und zur Verarbeitung uebernommen.",
                            bericht.getWarnings()));
        }

        // Reine Strukturfehler meldet eine Kasse als Syntaxfehler zurueck,
        // inhaltliche Beanstandungen als Zurueckweisung. Die Unterscheidung
        // entscheidet darueber, ob die Datei technisch neu erzeugt oder
        // fachlich korrigiert werden muss.
        boolean nurSyntax = bericht.getErrors().stream()
                .allMatch(m -> m.code().startsWith("SYNTAX_")
                        || m.code().startsWith("RAHMEN_")
                        || m.code().startsWith("UNT_")
                        || m.code().startsWith("UNZ_"));

        BillingOfficeResponseType typ = nurSyntax
                ? BillingOfficeResponseType.SYNTAX_ERROR
                : BillingOfficeResponseType.REJECTED;
        String kurztext = nurSyntax ? "Syntaxfehler" : "Abgelehnt";

        return new BillingOfficeResponse(typ, kurztext,
                protokoll("Die Lieferung wurde zurueckgewiesen.", bericht.getMessages()));
    }

    /**
     * Nimmt eine Datei entgegen und legt das Antwortprotokoll daneben ab.
     *
     * @param dtaDatei die eingegangene Lieferung
     * @return Pfad des geschriebenen Antwortprotokolls
     */
    public Path empfangeDatei(Path dtaDatei) throws IOException {
        Objects.requireNonNull(dtaDatei, "dtaDatei must not be null");
        String inhalt = Files.readString(dtaDatei, StandardCharsets.UTF_8);
        BillingOfficeResponse antwort = empfange(inhalt);

        Path protokollDatei = dtaDatei.resolveSibling(dtaDatei.getFileName() + PROTOKOLL_ENDUNG);
        Files.writeString(protokollDatei, antwort.getRawContent(), StandardCharsets.UTF_8);
        return protokollDatei;
    }

    private String protokoll(String kopfzeile, List<ValidationMessage> befunde) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add("Eingangsbestaetigung " + kassenname);
        zeilen.add("Eingang: " + LocalDateTime.now().format(PROTOKOLL_ZEIT));
        zeilen.add("");
        zeilen.add(kopfzeile);

        if (!befunde.isEmpty()) {
            zeilen.add("");
            zeilen.add("Beanstandungen:");
            for (ValidationMessage befund : befunde) {
                zeilen.add("  - [%s] %s %s".formatted(befund.code(), befund.ort(), befund.text()));
            }
        }
        return String.join(System.lineSeparator(), zeilen);
    }
}
