package de.gkvtransmitter.dispatch;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Ordnet die Rueckmeldung einer Kasse einer Bewertung zu.
 *
 * <p>Die bisherige Auswertung stand in {@code DtaDispatchService} und hatte
 * zwei Fehler, die beide in dieselbe Richtung wirkten - eine Lieferung konnte
 * als angenommen gelten, obwohl sie es nicht war:</p>
 *
 * <ol>
 *   <li>Die Reihenfolge war falsch. Auf {@code "fehler"} wurde vor
 *       {@code "technisch"} geprueft, so dass "Technischer Fehler" als
 *       fachliche Ablehnung galt. Die Einstufung als technischer Fehler war
 *       praktisch unerreichbar - beide erfordern aber unterschiedliche
 *       Reaktionen: eine Ablehnung will korrigiert werden, ein technischer
 *       Fehler nur erneut gesendet.</li>
 *   <li>Gesucht wurde nach Teilzeichenketten. {@code "ok"} kommt in
 *       "Protokoll" vor, weshalb eine Rueckmeldung wie "Fehlerprotokoll
 *       liegt vor" als Annahme gewertet werden konnte.</li>
 * </ol>
 *
 * <p>Die Regeln werden hier in fester Reihenfolge von der spezifischsten zur
 * allgemeinsten geprueft, und die Suchbegriffe sind an Wortgrenzen gebunden.</p>
 */
public final class BillingOfficeResponseParser {

    /**
     * Ein Suchbegriff mit der Bewertung, die er ausloest.
     *
     * @param typ       die Einstufung
     * @param kurztext  Beschreibung fuer die Anzeige
     * @param muster    an Wortgrenzen gebundenes Suchmuster
     */
    private record Regel(BillingOfficeResponseType typ, String kurztext, Pattern muster) {

        static Regel of(BillingOfficeResponseType typ, String kurztext, String... begriffe) {
            String alternativen = String.join("|", begriffe);
            // \b bindet an Wortgrenzen, damit "ok" nicht in "Protokoll" trifft.
            // UNICODE_CASE ist noetig, damit Umlaute richtig klein geschrieben werden.
            Pattern muster = Pattern.compile("\\b(" + alternativen + ")\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            return new Regel(typ, kurztext, muster);
        }

        boolean trifftAuf(String text) {
            return muster.matcher(text).find();
        }
    }

    /**
     * Die Regeln in Pruefreihenfolge.
     *
     * <p>Spezifische Begriffe stehen vor allgemeinen. Insbesondere wird
     * "technischer fehler" vor dem allgemeinen "fehler" geprueft, sonst waere
     * die Einstufung als technischer Fehler nicht erreichbar.</p>
     */
    private static final List<Regel> REGELN = List.of(
            Regel.of(BillingOfficeResponseType.SYNTAX_ERROR, "Syntaxfehler",
                    "syntaxfehler", "syntax-fehler", "ungültige länge", "ungueltige laenge",
                    "feldverschiebung", "formatfehler"),

            Regel.of(BillingOfficeResponseType.TECHNICAL_ERROR, "Technischer Fehler",
                    "technischer fehler", "technische störung", "technische stoerung",
                    "übertragungsfehler", "uebertragungsfehler",
                    "timeout", "zeitüberschreitung", "zeitueberschreitung",
                    "nicht erreichbar", "verbindungsfehler"),

            Regel.of(BillingOfficeResponseType.REJECTED, "Abgelehnt",
                    "abgelehnt", "zurückgewiesen", "zurueckgewiesen", "abgewiesen",
                    "zurückweisung", "zurueckweisung", "beanstandet",
                    // Zusammengesetzte Begriffe muessen einzeln aufgefuehrt
                    // werden: "fehler" allein trifft wegen der Wortgrenze nicht
                    // in "Fehlerprotokoll".
                    "fehlerprotokoll", "fehlermeldung", "fehlernachricht", "fehler"),

            Regel.of(BillingOfficeResponseType.ACCEPTED, "Angenommen",
                    "angenommen", "erfolgreich", "akzeptiert", "verarbeitet", "ok"));

    /**
     * Wertet eine Rueckmeldung aus.
     *
     * @param rohtext die Antwort der Kasse, darf {@code null} sein
     * @return die Einstufung, im Zweifel {@link BillingOfficeResponseType#UNKNOWN}
     */
    public BillingOfficeResponse parse(String rohtext) {
        if (rohtext == null || rohtext.isBlank()) {
            return new BillingOfficeResponse(BillingOfficeResponseType.UNKNOWN, "Leere Antwort", rohtext);
        }

        for (Regel regel : REGELN) {
            if (regel.trifftAuf(rohtext)) {
                return new BillingOfficeResponse(regel.typ(), regel.kurztext(), rohtext);
            }
        }

        // Bewusst nicht als Annahme gewertet: eine Rueckmeldung, die sich nicht
        // einordnen laesst, muss ein Mensch ansehen.
        return new BillingOfficeResponse(BillingOfficeResponseType.UNKNOWN, "Unbekannte Antwort", rohtext);
    }

    /** Wertet die Rueckmeldung aus und wirft, wenn sie keine Annahme ist. */
    public BillingOfficeResponse parseOderScheitern(String rohtext) {
        BillingOfficeResponse antwort = parse(rohtext);
        if (antwort.getType() != BillingOfficeResponseType.ACCEPTED) {
            throw new IllegalStateException(
                    "Die Kasse hat die Lieferung nicht angenommen: " + antwort.getMessage());
        }
        return Objects.requireNonNull(antwort);
    }
}
