package de.gkvtransmitter.validator.rules;

import java.util.List;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft Rechnungsnummer und Belegnummer.
 *
 * <p>Beide waren bis zum 18.09.2026 fest im Quelltext: Das {@code REC} trug
 * {@code 00000000:0}, die Belegnummer im {@code INV} war
 * {@code "HEB" + Jahr/Monat + Kennnummer der Teilnehmerin}. Die Kasse haette
 * jede Lieferung unter derselben Rechnungsnummer verbucht.</p>
 *
 * <h2>Was die Anlage verlangt</h2>
 *
 * <p>Anlage 1, Abschnitt 5.5.2:</p>
 *
 * <ul>
 *   <li><b>Sammel-Rechnungsnummer</b> ({@code REC}, {@code ..14} AN, Pflicht).
 *       Die Einzel-Rechnungsnummer daneben wird auf {@code 0} gesetzt, solange
 *       keine Abrechnungsstelle mit Inkassovollmacht absendet.</li>
 *   <li><b>Belegnummer</b> ({@code INV}, {@code ..10} AN, Pflicht):
 *       "Eindeutige Nummer innerhalb der Gesamtrechnung".</li>
 * </ul>
 *
 * <p><b>Diese Regel ist der Ausgang der Sperre</b>, die
 * {@link de.gkvtransmitter.dta.Rechnungsangaben#OHNE} setzt: Ein Lauf ohne
 * vergebene Rechnungsnummer erzeugt die alte {@code 00000000} und wird hier
 * angehalten, statt mit einer Nullnummer bei der Kasse aufzuschlagen.</p>
 */
public final class RechnungsangabenRegel implements ValidationRule {

    private static final String REC = "REC";
    private static final String INV = "INV";

    /** Sammel- und Einzelrechnungsnummer stehen zusammen im ersten Element. */
    private static final int REC_NUMMER = 0;
    /** Das Rechnungsdatum folgt darauf. */
    private static final int REC_DATUM = 1;
    /** Die Belegnummer ist das vierte Element des INV. */
    private static final int INV_BELEGNUMMER = 3;

    /** Hoechstlaenge der Sammel-Rechnungsnummer, Anlage 1 Abschnitt 5.5.2. */
    private static final int MAX_RECHNUNGSNUMMER = 14;
    /** Hoechstlaenge der Belegnummer. */
    private static final int MAX_BELEGNUMMER = 10;

    @Override
    public String getName() {
        return "Rechnungsangaben";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        for (DtaSegment rec : document.mitTag(REC)) {
            pruefeRechnungsnummer(rec, bericht);
        }
        List<DtaSegment> invs = document.mitTag(INV);
        for (DtaSegment inv : invs) {
            pruefeBelegnummer(inv, bericht);
        }
        pruefeEindeutigkeit(invs, bericht);
    }

    /**
     * Die Sammel-Rechnungsnummer muss vergeben und darf nicht null sein.
     *
     * <p>Eine Nummer aus lauter Nullen ist keine: Zwei Rechnungen mit
     * derselben Nummer sind fuer die Kasse eine, und eine Korrektur darauf
     * verweist ins Leere.</p>
     */
    private static void pruefeRechnungsnummer(DtaSegment rec, ValidationReport.Builder bericht) {
        String feld = rec.element(REC_NUMMER).trim();
        String sammelnummer = feld.contains(":") ? feld.substring(0, feld.indexOf(':')) : feld;

        if (sammelnummer.isEmpty()) {
            bericht.fehler("RECHNUNGSNUMMER_FEHLT", rec.ort());
            return;
        }
        if (sammelnummer.chars().allMatch(zeichen -> zeichen == '0')) {
            bericht.fehler("RECHNUNGSNUMMER_NULL", rec.ort());
        }
        if (sammelnummer.length() > MAX_RECHNUNGSNUMMER) {
            bericht.fehler("RECHNUNGSNUMMER_ZU_LANG", rec.ort(), sammelnummer.length(), MAX_RECHNUNGSNUMMER);
        }
        if (rec.element(REC_DATUM).trim().isEmpty()) {
            bericht.fehler("RECHNUNGSDATUM_FEHLT", rec.ort());
        }
    }

    /** Die Belegnummer ist Pflicht und hoechstens zehnstellig. */
    private static void pruefeBelegnummer(DtaSegment inv, ValidationReport.Builder bericht) {
        String belegnummer = inv.element(INV_BELEGNUMMER).trim();
        if (belegnummer.isEmpty()) {
            bericht.fehler("BELEGNUMMER_FEHLT", inv.ort());
            return;
        }
        if (belegnummer.length() > MAX_BELEGNUMMER) {
            bericht.fehler("BELEGNUMMER_ZU_LANG", inv.ort(), belegnummer, belegnummer.length(), MAX_BELEGNUMMER);
        }
    }

    /**
     * Zweimal dieselbe Belegnummer in einer Rechnung.
     *
     * <p>"Eindeutige Nummer innerhalb der Gesamtrechnung" - der Punkt, an dem
     * die Anlage weniger verlangt, als die freie Bildung vorgab, und genau
     * diesen einen Punkt dafuer wirklich.</p>
     */
    private static void pruefeEindeutigkeit(List<DtaSegment> invs, ValidationReport.Builder bericht) {
        java.util.Set<String> gesehen = new java.util.HashSet<>();
        for (DtaSegment inv : invs) {
            String belegnummer = inv.element(INV_BELEGNUMMER).trim();
            if (!belegnummer.isEmpty() && !gesehen.add(belegnummer)) {
                bericht.fehler("BELEGNUMMER_DOPPELT", inv.ort(), belegnummer);
            }
        }
    }
}
