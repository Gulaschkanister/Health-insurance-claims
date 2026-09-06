package de.gkvtransmitter.validator.rules;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Optional;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft, dass eine Leistungszeile ueberhaupt etwas abrechnet.
 *
 * <p>Eine {@code ENF}-Zeile mit dem Einzelbetrag 0,00 oder der Menge 0,00 ist
 * in sich stimmig: die Fallsumme im {@code BES} ist dann ebenfalls 0,00, und
 * die {@link BetragskonsistenzRegel} findet nichts zu beanstanden. Genau das
 * macht sie gefaehrlich - eine Lieferung geht hinaus, die keine Forderung
 * enthaelt, und niemand merkt es, bis die Bezahlung ausbleibt.</p>
 *
 * <p><b>Woher eine solche Zeile kommt.</b> {@code Leistungsparameter} liest den
 * Preis aus der Blaupause und faellt auf {@code VORBELEGUNG} zurueck, wenn dort
 * keiner steht. Diese Rueckfallebene stand bis zum 06.09.2026 auf 15.000,00 -
 * dem Betrag aus der Beispieldatei. Eine Blaupause ohne Preis rechnete damit
 * jeden Termin mit 15.000,00 ab, und weil auch das in sich stimmig war, hielt
 * es keine Regel auf. Der Betrag ist jetzt 0,00, und diese Regel haelt es
 * auf.</p>
 *
 * <p>Beides zusammen ist die Absicht: <b>ein erfundener Wert, der plausibel
 * aussieht, ist schlimmer als einer, der nicht durchkommt.</b> Dieselbe
 * Entscheidung wie beim fehlenden Geburtsdatum in
 * {@code DtaFactory.buildNadSegment} - lieber eine Abrechnung, die nicht
 * losgeht und sagt warum.</p>
 *
 * <p>Die Oberflaeche laesst eine Blaupause ohne Preis gar nicht erst speichern
 * ({@code BlaupausenMaske.speichere}). Diese Regel ist die zweite Linie: sie
 * greift auch bei Blaupausen, die vor dieser Pruefung angelegt wurden, bei
 * unlesbarer Nutzlast und bei Nachrichten aus fremder Quelle.</p>
 */
public final class LeistungspositionRegel implements ValidationRule {

    private static final String ENF = "ENF";

    /** Die Menge steht im ENF an vierter Stelle, der Einzelbetrag an fuenfter. */
    private static final int ENF_MENGE = 3;
    private static final int ENF_BETRAG = 4;

    @Override
    public String getName() {
        return "Leistungsposition";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        for (DtaSegment enf : document.mitTag(ENF)) {
            pruefeWert(enf, ENF_BETRAG, bericht, "ENF_BETRAG_NULL",
                    "Der Einzelbetrag der Leistungsposition ist 0,00."
                            + " Die Blaupause fuehrt keinen Preis - so abgerechnet waere die"
                            + " Forderung null.");
            pruefeWert(enf, ENF_MENGE, bericht, "ENF_MENGE_NULL",
                    "Die Menge der Leistungsposition ist 0,00."
                            + " Ohne Termine gibt es nichts abzurechnen.");
        }
    }

    /**
     * Meldet einen Wert, der auf null steht.
     *
     * <p>Ein <em>unlesbarer</em> oder fehlender Wert wird hier bewusst
     * uebergangen: den meldet bereits {@link BetragskonsistenzRegel}, und zwei
     * Beanstandungen zur selben Stelle machen den Bericht nur laenger.</p>
     */
    private void pruefeWert(DtaSegment enf, int position, ValidationReport.Builder bericht,
            String code, String meldung) {
        Optional<BigDecimal> wert = lese(enf.element(position).trim());
        if (wert.isPresent() && wert.get().compareTo(BigDecimal.ZERO) == 0) {
            bericht.error(code, enf.ort(), meldung);
        }
    }

    /** Liest einen Betrag in deutscher Schreibweise, siehe {@link BetragskonsistenzRegel}. */
    private static Optional<BigDecimal> lese(String wert) {
        if (wert.isEmpty()) {
            return Optional.empty();
        }
        DecimalFormat format = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.GERMAN));
        format.setParseBigDecimal(true);
        ParsePosition position = new ParsePosition(0);
        Number gelesen = format.parse(wert, position);
        if (gelesen == null || position.getIndex() != wert.length()) {
            return Optional.empty();
        }
        return Optional.of((BigDecimal) gelesen);
    }
}
