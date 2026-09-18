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
            bericht.error("RECHNUNGSNUMMER_FEHLT", rec.ort(),
                    "Im REC-Segment fehlt die Sammel-Rechnungsnummer. Sie ist Pflicht"
                            + " (Anlage 1, Abschnitt 5.5.2) und die Angabe, auf die sich eine"
                            + " spaetere Korrektur bezieht.");
            return;
        }
        if (sammelnummer.chars().allMatch(zeichen -> zeichen == '0')) {
            bericht.error("RECHNUNGSNUMMER_NULL", rec.ort(),
                    "Die Sammel-Rechnungsnummer besteht nur aus Nullen. Sie wurde nicht"
                            + " vergeben - jede Rechnung traegt dann dieselbe Nummer, und eine"
                            + " Korrektur laesst sich keiner davon zuordnen.");
        }
        if (sammelnummer.length() > MAX_RECHNUNGSNUMMER) {
            bericht.error("RECHNUNGSNUMMER_ZU_LANG", rec.ort(),
                    "Die Sammel-Rechnungsnummer hat " + sammelnummer.length()
                            + " Stellen; hoechstens " + MAX_RECHNUNGSNUMMER
                            + " sind zulaessig (Anlage 1, Abschnitt 5.5.2).");
        }
        if (rec.element(REC_DATUM).trim().isEmpty()) {
            bericht.error("RECHNUNGSDATUM_FEHLT", rec.ort(),
                    "Im REC-Segment fehlt das Rechnungsdatum. Es ist Pflicht und gehoert"
                            + " zusammen mit der Rechnungsnummer zu den Angaben, auf die eine"
                            + " Korrektur verweist.");
        }
    }

    /** Die Belegnummer ist Pflicht und hoechstens zehnstellig. */
    private static void pruefeBelegnummer(DtaSegment inv, ValidationReport.Builder bericht) {
        String belegnummer = inv.element(INV_BELEGNUMMER).trim();
        if (belegnummer.isEmpty()) {
            bericht.error("BELEGNUMMER_FEHLT", inv.ort(),
                    "Im INV-Segment fehlt die Belegnummer. Sie ist Pflicht und muss"
                            + " innerhalb der Gesamtrechnung eindeutig sein"
                            + " (Anlage 1, Abschnitt 5.5.2).");
            return;
        }
        if (belegnummer.length() > MAX_BELEGNUMMER) {
            bericht.error("BELEGNUMMER_ZU_LANG", inv.ort(),
                    "Die Belegnummer '" + belegnummer + "' hat " + belegnummer.length()
                            + " Stellen; hoechstens " + MAX_BELEGNUMMER + " sind zulaessig.");
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
                bericht.error("BELEGNUMMER_DOPPELT", inv.ort(),
                        "Die Belegnummer '" + belegnummer + "' kommt in dieser Rechnung mehrfach"
                                + " vor. Sie muss innerhalb der Gesamtrechnung eindeutig sein"
                                + " (Anlage 1, Abschnitt 5.5.2).");
            }
        }
    }
}
