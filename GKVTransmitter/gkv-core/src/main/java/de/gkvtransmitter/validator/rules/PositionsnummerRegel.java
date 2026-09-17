package de.gkvtransmitter.validator.rules;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft die Abrechnungspositionsnummer gegen den Abrechnungscode.
 *
 * <p>Die Positionsnummer hat je Leistungsbereich eine eigene Laenge. Fuer
 * <b>Hebammenhilfe (Abrechnungscode 50)</b> sind es nach Anlage 3, Abschnitt
 * 8.2.6 <b>vier oder fuenf Stellen</b> - vier nur bei Betriebskostenpauschalen
 * fuer von Hebammen geleitete Einrichtungen, sonst fuenf. Seit Leistungsdatum
 * 01.11.2025 gilt das fuenfstellige Verzeichnis.</p>
 *
 * <p><b>Warum das eine Warnung ist und kein Fehler.</b> Die Vorbelegung des
 * Projekts fuehrt eine neunstellige Nummer, die aus der Beispieldatei stammt
 * und dort zum Abrechnungscode 61 gehoerte. Eine richtige laesst sich nicht
 * hinschreiben: das bundeseinheitliche Positionsnummernverzeichnis der
 * Hebammenhilfe-Verguetungsvereinbarung liegt dem Projekt nicht vor. Ein
 * Fehler wuerde damit jede Abrechnung anhalten, ohne dass jemand sie loesen
 * koennte - eine Sperre ohne Ausgang. Die Warnung nennt die Luecke bei jedem
 * Lauf und laesst die Arbeit weitergehen.</p>
 *
 * <p>Sie ersetzt keine fachliche Pruefung. <b>Ob eine Nummer richtiger Laenge
 * auch vertraglich vereinbart ist, entscheidet die Kasse in Pruefstufe 4</b>,
 * und das kann kein Programm hier vorwegnehmen.</p>
 */
public final class PositionsnummerRegel implements ValidationRule {

    private static final String ENF = "ENF";

    /** Leistungserbringergruppe {@code Abrechnungscode:Tarifkennzeichen}. */
    private static final int ENF_GRUPPE = 1;

    /** Die Abrechnungspositionsnummer steht dahinter. */
    private static final int ENF_POSITION = 2;

    /** Das Leistungsdatum, {@code yyyyMMdd}. */
    private static final int ENF_LEISTUNGSDATUM = 5;

    /** Abrechnungscode Hebamme/Entbindungspfleger, Anlage 3 Abschnitt 8.1.5. */
    static final String HEBAMME = "50";

    private static final int KUERZESTE = 4;
    private static final int LAENGSTE = 5;

    /**
     * Die vierte Stelle einer fuenfstelligen Nummer, 0-basiert.
     *
     * <p>Sie traegt den Zuschlag nach § 3 der Anlage 1.1 zum Vertrag nach
     * § 134a SGB V und kann nur {@code 0} (kein Zuschlag) oder {@code 1} (mit
     * Zuschlag) sein - Anlage 3, Abschnitt 8.2.6.</p>
     */
    private static final int STELLE_ZUSCHLAG = 3;

    /**
     * Bis zu diesem Leistungsdatum galt das alte, ausschliesslich vierstellige
     * Verzeichnis (Anlage 3, Abschnitt 8.2.6).
     */
    private static final LocalDate LETZTER_TAG_VIERSTELLIG = LocalDate.of(2025, 10, 31);

    @Override
    public String getName() {
        return "Positionsnummer";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        for (DtaSegment enf : document.mitTag(ENF)) {
            String gruppe = enf.element(ENF_GRUPPE).trim();
            String position = enf.element(ENF_POSITION).trim();
            if (!HEBAMME.equals(abrechnungscode(gruppe)) || position.isEmpty()) {
                continue;
            }
            if (position.length() < KUERZESTE || position.length() > LAENGSTE) {
                bericht.warning("POSITION_HEBAMME_LAENGE", enf.ort(),
                        "Die Abrechnungspositionsnummer " + position + " hat " + position.length()
                                + " Stellen. Fuer Hebammenhilfe sind vier oder fuenf vorgesehen"
                                + " (Anlage 3, Abschnitt 8.2.6). Die Kasse weist die Zeile in"
                                + " Pruefstufe 3 zurueck.");
                continue;
            }
            pruefeAufbau(position, enf, bericht);
            pruefeUebergang(position, enf, bericht);
        }
    }

    /**
     * Die vierte Stelle einer fuenfstelligen Nummer traegt den Zuschlag.
     *
     * <p>Ob eine Nummer im Verzeichnis <em>steht</em>, laesst sich ohne das
     * Verzeichnis nicht sagen. Ob ihre vierte Stelle eine zulaessige
     * Auspraegung traegt, sehr wohl - dafuer genuegt Anlage 3.</p>
     */
    private static void pruefeAufbau(String position, DtaSegment enf, ValidationReport.Builder bericht) {
        if (position.length() != LAENGSTE) {
            return;
        }
        char zuschlag = position.charAt(STELLE_ZUSCHLAG);
        if (zuschlag != '0' && zuschlag != '1') {
            bericht.warning("POSITION_HEBAMME_ZUSCHLAG", enf.ort(),
                    "Die Abrechnungspositionsnummer " + position + " traegt an vierter Stelle '"
                            + zuschlag + "'. Dort steht der Zuschlag nach § 3 der Anlage 1.1 zum"
                            + " Vertrag nach § 134a SGB V, und der kennt nur 0 (kein Zuschlag)"
                            + " oder 1 (mit Zuschlag) - Anlage 3, Abschnitt 8.2.6.");
        }
    }

    /**
     * Vierstellige Nummern sind seit dem 01.11.2025 die Ausnahme.
     *
     * <p>Bis zum Leistungsdatum 31.10.2025 galt das alte Verzeichnis mit
     * ausschliesslich vierstelligen Nummern. Danach sind vier Stellen nur noch
     * bei Betriebskostenpauschalen fuer von Hebammen geleitete Einrichtungen
     * vorgesehen - und ob es sich um eine solche handelt, steht in der
     * Nachricht nicht. Deshalb ein Hinweis und keine Beanstandung.</p>
     */
    private static void pruefeUebergang(String position, DtaSegment enf, ValidationReport.Builder bericht) {
        if (position.length() != KUERZESTE) {
            return;
        }
        LocalDate leistungsdatum = datumAus(enf.element(ENF_LEISTUNGSDATUM).trim());
        if (leistungsdatum == null || !leistungsdatum.isAfter(LETZTER_TAG_VIERSTELLIG)) {
            return;
        }
        bericht.warning("POSITION_HEBAMME_VIERSTELLIG", enf.ort(),
                "Die Abrechnungspositionsnummer " + position + " ist vierstellig, das Leistungsdatum"
                        + " liegt aber nach dem 31.10.2025. Seither gilt das fuenfstellige"
                        + " Verzeichnis; vier Stellen sind nur noch bei Betriebskostenpauschalen"
                        + " fuer von Hebammen geleitete Einrichtungen vorgesehen"
                        + " (Anlage 3, Abschnitt 8.2.6).");
    }

    /**
     * Das Leistungsdatum, oder {@code null}, wenn es sich nicht lesen laesst.
     *
     * <p>Ein unlesbares Datum wird hier <b>nicht</b> beanstandet: Diese Regel
     * handelt von der Positionsnummer. Sie darf daran aber auch nicht
     * abstuerzen - dann kaeme keine der uebrigen Pruefungen mehr dazu.</p>
     */
    private static LocalDate datumAus(String text) {
        try {
            return LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException nichtLesbar) {
            return null;
        }
    }

    /** Der Teil vor dem Doppelpunkt; dahinter steht das Tarifkennzeichen. */
    private static String abrechnungscode(String gruppe) {
        int trenner = gruppe.indexOf(':');
        return trenner < 0 ? gruppe : gruppe.substring(0, trenner);
    }
}
