package de.gkvtransmitter.validator.rules;

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

    /** Abrechnungscode Hebamme/Entbindungspfleger, Anlage 3 Abschnitt 8.1.5. */
    static final String HEBAMME = "50";

    private static final int KUERZESTE = 4;
    private static final int LAENGSTE = 5;

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
            }
        }
    }

    /** Der Teil vor dem Doppelpunkt; dahinter steht das Tarifkennzeichen. */
    private static String abrechnungscode(String gruppe) {
        int trenner = gruppe.indexOf(':');
        return trenner < 0 ? gruppe : gruppe.substring(0, trenner);
    }
}
