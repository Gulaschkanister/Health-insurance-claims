package de.gkvtransmitter.validator.rules;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft, ob die Summen zu den Einzelposten passen.
 *
 * <p>Die Rechnungssumme steht mehrfach in der Lieferung: als Summe je Fall im
 * {@code BES}, als Gesamtsumme im {@code GES} der SLGA-Nachricht, und
 * aufgeschluesselt in den Leistungspositionen der {@code ENF}-Segmente. Weichen
 * diese Angaben voneinander ab, weist die Kasse die Abrechnung zurueck.</p>
 *
 * <p>Die Regel meldet Abweichungen als Fehler, weil eine falsche Summe die
 * Abrechnung wertlos macht - nicht als Warnung, die man uebergehen koennte.</p>
 */
public final class BetragskonsistenzRegel implements ValidationRule {

    private static final String BES = "BES";
    private static final String GES = "GES";
    private static final String ENF = "ENF";

    /** Der Rechnungsbetrag steht im BES an erster Stelle. */
    private static final int BES_BETRAG = 0;
    /** Der Summenschluessel steht im GES an erster Stelle. */
    private static final int GES_SCHLUESSEL = 0;
    /** Der Bruttobetrag steht im GES an zweiter Stelle. */
    private static final int GES_BETRAG = 1;
    /** Der Einzelbetrag steht im ENF an fuenfter Stelle. */
    private static final int ENF_BETRAG = 4;
    /** Die Menge steht im ENF an vierter Stelle. */
    private static final int ENF_MENGE = 3;

    /** Summenschluessel der Gesamtsumme ueber alle Rechnungen. */
    private static final String SCHLUESSEL_GESAMT = "99";

    @Override
    public String getName() {
        return "Betragskonsistenz";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        pruefeEnfGegenBes(document, bericht);
        pruefeGesGegenBes(document, bericht);
    }

    /** Menge mal Einzelbetrag der Leistungspositionen muss die Fallsumme ergeben. */
    private void pruefeEnfGegenBes(DtaDocument document, ValidationReport.Builder bericht) {
        List<DtaSegment> enfSegmente = document.mitTag(ENF);
        Optional<DtaSegment> bes = document.erstesMitTag(BES);
        if (enfSegmente.isEmpty() || bes.isEmpty()) {
            return;
        }

        BigDecimal summeDerPosten = BigDecimal.ZERO;
        for (DtaSegment enf : enfSegmente) {
            Optional<BigDecimal> betrag = betrag(enf, ENF_BETRAG, bericht, "ENF_BETRAG_UNGUELTIG");
            Optional<BigDecimal> menge = betrag(enf, ENF_MENGE, bericht, "ENF_MENGE_UNGUELTIG");
            if (betrag.isEmpty() || menge.isEmpty()) {
                return;
            }
            summeDerPosten = summeDerPosten.add(betrag.get().multiply(menge.get()));
        }

        Optional<BigDecimal> fallsumme = betrag(bes.get(), BES_BETRAG, bericht, "BES_BETRAG_UNGUELTIG");
        if (fallsumme.isEmpty()) {
            return;
        }
        if (fallsumme.get().compareTo(summeDerPosten) != 0) {
            bericht.error("BETRAG_ENF_BES", bes.get().ort(),
                    "Die Rechnungssumme im BES (%s) entspricht nicht der Summe der Leistungspositionen (%s)."
                            .formatted(formatiere(fallsumme.get()), formatiere(summeDerPosten)));
        }
    }

    /** Die Gesamtsumme im GES muss die Summe der Fallsummen sein. */
    private void pruefeGesGegenBes(DtaDocument document, ValidationReport.Builder bericht) {
        List<DtaSegment> besSegmente = document.mitTag(BES);
        List<DtaSegment> gesSegmente = document.mitTag(GES);
        if (besSegmente.isEmpty() || gesSegmente.isEmpty()) {
            return;
        }

        BigDecimal summeDerFaelle = BigDecimal.ZERO;
        for (DtaSegment bes : besSegmente) {
            Optional<BigDecimal> betrag = betrag(bes, BES_BETRAG, bericht, "BES_BETRAG_UNGUELTIG");
            if (betrag.isEmpty()) {
                return;
            }
            summeDerFaelle = summeDerFaelle.add(betrag.get());
        }

        for (DtaSegment ges : gesSegmente) {
            if (!SCHLUESSEL_GESAMT.equals(ges.element(GES_SCHLUESSEL).trim())) {
                continue;
            }
            Optional<BigDecimal> gesamt = betrag(ges, GES_BETRAG, bericht, "GES_BETRAG_UNGUELTIG");
            if (gesamt.isPresent() && gesamt.get().compareTo(summeDerFaelle) != 0) {
                bericht.error("BETRAG_GES_BES", ges.ort(),
                        "Die Gesamtsumme im GES (%s) entspricht nicht der Summe der Rechnungen (%s)."
                                .formatted(formatiere(gesamt.get()), formatiere(summeDerFaelle)));
            }
        }
    }

    private Optional<BigDecimal> betrag(DtaSegment segment, int position,
            ValidationReport.Builder bericht, String code) {
        String roh = segment.element(position).trim();
        if (roh.isEmpty()) {
            bericht.error(code, segment.ort(),
                    "Im Segment %s fehlt ein Betrag an Position %d.".formatted(segment.tag(), position + 1));
            return Optional.empty();
        }
        try {
            return Optional.of(lese(roh));
        } catch (ParseException e) {
            bericht.error(code, segment.ort(),
                    "Der Wert '%s' im Segment %s ist kein gueltiger Betrag."
                            .formatted(roh, segment.tag()));
            return Optional.empty();
        }
    }

    /**
     * Liest einen Betrag in deutscher Schreibweise.
     *
     * <p>Betraege stehen im DTA mit Komma als Dezimaltrennzeichen. Ein Parsen
     * ueber {@code new BigDecimal(String)} wuerde daran scheitern.</p>
     */
    private static BigDecimal lese(String wert) throws ParseException {
        DecimalFormat format = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.GERMAN));
        format.setParseBigDecimal(true);
        java.text.ParsePosition position = new java.text.ParsePosition(0);
        Number gelesen = format.parse(wert, position);
        if (gelesen == null || position.getIndex() != wert.length()) {
            throw new ParseException("Kein gueltiger Betrag: " + wert, position.getIndex());
        }
        return (BigDecimal) gelesen;
    }

    private static String formatiere(BigDecimal betrag) {
        return String.format(Locale.GERMAN, "%,.2f", betrag);
    }
}
