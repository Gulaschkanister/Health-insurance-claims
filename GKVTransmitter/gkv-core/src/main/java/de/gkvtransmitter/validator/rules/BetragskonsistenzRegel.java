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

    /**
     * Summenschluessel 00: Gesamtsumme aller Status.
     *
     * <p>Laut {@code Information/codes/04_summen_und_statuscodes.json} ist das
     * die Summe aller BES. Eine Abweichung ist ein Fehler.</p>
     */
    private static final String SCHLUESSEL_GESAMTSUMME = "00";

    /**
     * Summenschluessel 99: nicht zuzuordnende Status.
     *
     * <p>Dieser Wert richtet sich nach der Pruefvorgabe der Kasse und ist in
     * der Referenzdatei mit der Gesamtsumme identisch. Eine Abweichung wird
     * deshalb nur als Hinweis gemeldet - sie kann fachlich gewollt sein.</p>
     */
    private static final String SCHLUESSEL_NICHT_ZUZUORDNEN = "99";

    @Override
    public String getName() {
        return "Betragskonsistenz";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        pruefeEnfGegenBes(document, bericht);
        pruefeGesGegenBes(document, bericht);
    }

    /**
     * Menge mal Einzelbetrag der Leistungspositionen muss die Fallsumme ergeben
     * — <b>je Abrechnungsfall</b>.
     *
     * <p>Bis zum 06.09.2026 summierte diese Methode <em>alle</em> ENF des
     * Dokuments und verglich sie mit dem <em>ersten</em> BES. Bei einer Datei
     * mit mehreren Abrechnungsfaellen war das Ergebnis in beide Richtungen
     * falsch: entweder wurde ein Fehler gemeldet, den es nicht gibt (die Summe
     * aller Posten gegen die Summe eines Falles), oder eine echte Abweichung
     * blieb verdeckt. Die Schwestermethode {@code pruefeGesGegenBes} iterierte
     * daneben ueber alle BES — die beiden waren sich uneins.</p>
     *
     * <p>Ein BES schliesst seinen Fall ab; die ENF davor gehoeren dazu. Die
     * Reihenfolge der Segmente traegt diese Zuordnung, und {@code getSegments}
     * gibt sie unveraendert heraus. Deshalb wird hier durchgelaufen statt nach
     * Tags gesucht.</p>
     */
    private void pruefeEnfGegenBes(DtaDocument document, ValidationReport.Builder bericht) {
        BigDecimal summeDerPosten = BigDecimal.ZERO;
        boolean postenGesehen = false;

        for (DtaSegment segment : document.getSegments()) {
            if (ENF.equals(segment.tag())) {
                Optional<BigDecimal> betrag = betrag(segment, ENF_BETRAG, bericht,
                        "ENF_BETRAG_FEHLT", "ENF_BETRAG_UNGUELTIG");
                Optional<BigDecimal> menge = betrag(segment, ENF_MENGE, bericht,
                        "ENF_MENGE_FEHLT", "ENF_MENGE_UNGUELTIG");
                if (betrag.isEmpty() || menge.isEmpty()) {
                    return;
                }
                summeDerPosten = summeDerPosten.add(betrag.get().multiply(menge.get()));
                postenGesehen = true;
                continue;
            }
            if (!BES.equals(segment.tag())) {
                continue;
            }
            // Ein BES ohne vorangehende Leistungszeilen ist nicht Sache dieser
            // Regel - dass ueberhaupt ein ENF da sein muss, prueft der Rahmen.
            if (postenGesehen) {
                vergleiche(segment, summeDerPosten, bericht);
            }
            summeDerPosten = BigDecimal.ZERO;
            postenGesehen = false;
        }
    }

    private void vergleiche(DtaSegment bes, BigDecimal summeDerPosten, ValidationReport.Builder bericht) {
        Optional<BigDecimal> fallsumme = betrag(bes, BES_BETRAG, bericht, "BES_BETRAG_FEHLT", "BES_BETRAG_UNGUELTIG");
        if (fallsumme.isEmpty()) {
            return;
        }
        if (fallsumme.get().compareTo(summeDerPosten) != 0) {
            bericht.fehler("BETRAG_ENF_BES", bes.ort(), formatiere(fallsumme.get()), formatiere(summeDerPosten));
        }
    }

    /** Die Gesamtsumme im GES muss die Summe der Fallsummen sein. */
    private void pruefeGesGegenBes(DtaDocument document, ValidationReport.Builder bericht) {
        List<DtaSegment> besSegmente = document.mitTag(BES);
        List<DtaSegment> gesSegmente = document.mitTag(GES);
        if (besSegmente.isEmpty() || gesSegmente.isEmpty()) {
            return;
        }

        Optional<BigDecimal> summe = summeDerFaelle(besSegmente, bericht);
        if (summe.isEmpty()) {
            return;
        }
        vergleicheGes(gesSegmente, summe.get(), bericht);
    }

    /**
     * Die Summe aller Fallsummen.
     *
     * <p>Herausgeloest, weil {@code pruefeGesGegenBes} mit beiden Schleifen auf
     * zyklomatische Komplexitaet 11 kam. Leer heisst: ein Betrag war unlesbar,
     * das ist bereits gemeldet, und weiterrechnen hiesse eine Abweichung
     * melden, die aus dem eigenen Rechenfehler stammt.</p>
     */
    private Optional<BigDecimal> summeDerFaelle(List<DtaSegment> besSegmente,
            ValidationReport.Builder bericht) {
        BigDecimal summeDerFaelle = BigDecimal.ZERO;
        for (DtaSegment bes : besSegmente) {
            Optional<BigDecimal> betrag = betrag(bes, BES_BETRAG, bericht, "BES_BETRAG_FEHLT", "BES_BETRAG_UNGUELTIG");
            if (betrag.isEmpty()) {
                return Optional.empty();
            }
            summeDerFaelle = summeDerFaelle.add(betrag.get());
        }
        return Optional.of(summeDerFaelle);
    }

    /** Haelt jede Gesamtsumme gegen die Summe der Faelle. */
    private void vergleicheGes(List<DtaSegment> gesSegmente, BigDecimal summeDerFaelle,
            ValidationReport.Builder bericht) {
        for (DtaSegment ges : gesSegmente) {
            String schluessel = ges.element(GES_SCHLUESSEL).trim();
            boolean istGesamtsumme = SCHLUESSEL_GESAMTSUMME.equals(schluessel);
            boolean istNichtZuzuordnen = SCHLUESSEL_NICHT_ZUZUORDNEN.equals(schluessel);
            if (!istGesamtsumme && !istNichtZuzuordnen) {
                continue;
            }

            Optional<BigDecimal> gesamt = betrag(ges, GES_BETRAG, bericht, "GES_BETRAG_FEHLT", "GES_BETRAG_UNGUELTIG");
            if (gesamt.isEmpty() || gesamt.get().compareTo(summeDerFaelle) == 0) {
                continue;
            }

            // Beide Befunde beschreiben dieselbe Abweichung und bekommen
            // deshalb dieselben Werte; der Katalog haengt beim Schluessel 99
            // den Satz an, dass das der Pruefvorgabe entsprechen kann.
            String kennung = istGesamtsumme ? "BETRAG_GES_BES" : "BETRAG_GES99_ABWEICHUNG";
            if (istGesamtsumme) {
                bericht.fehler(kennung, ges.ort(), schluessel,
                        formatiere(gesamt.get()), formatiere(summeDerFaelle));
            } else {
                bericht.warnung(kennung, ges.ort(), schluessel,
                        formatiere(gesamt.get()), formatiere(summeDerFaelle));
            }
        }
    }

    /**
     * @param codeFehlt      wenn gar kein Betrag dasteht
     * @param codeUngueltig  wenn dort etwas steht, das keiner ist
     */
    private Optional<BigDecimal> betrag(DtaSegment segment, int position,
            ValidationReport.Builder bericht, String codeFehlt, String codeUngueltig) {
        String roh = segment.element(position).trim();
        if (roh.isEmpty()) {
            bericht.fehler(codeFehlt, segment.ort(), segment.tag(), position + 1);
            return Optional.empty();
        }
        try {
            return Optional.of(lese(roh));
        } catch (ParseException e) {
            bericht.fehler(codeUngueltig, segment.ort(), roh, segment.tag());
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
