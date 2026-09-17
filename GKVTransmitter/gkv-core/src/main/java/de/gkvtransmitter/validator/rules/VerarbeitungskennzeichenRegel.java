package de.gkvtransmitter.validator.rules;

import java.util.List;
import java.util.Set;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft das Verarbeitungskennzeichen im {@code FKT} und das, was daran haengt.
 *
 * <p>Das Programm erzeugt heute ausschliesslich {@code FKT+01} - „Abrechnung
 * ohne Besonderheiten". Anlage 3, Abschnitt 8.1.7 kennt daneben vier weitere
 * Werte, und <b>ohne sie gibt es keinen Weg, auf eine Zurueckweisung zu
 * antworten</b>: Dieselbe Rechnung noch einmal als Erstabrechnung zu schicken
 * sieht fuer die Kasse wie eine Doppelabrechnung aus.</p>
 *
 * <p>Diese Regel baut den Weg nicht, sie sichert ihn ab. Sie gilt genauso fuer
 * eine fremde Datei, die eingelesen wird.</p>
 *
 * <h2>Was geprueft wird</h2>
 *
 * <ol>
 *   <li><b>Der Wert selbst</b> muss einer der fuenf aus Anlage 3, Abschnitt
 *       8.1.7 sein. Eine unbekannte Auspraegung faellt bei der Kasse in
 *       Pruefstufe 3.</li>
 *   <li><b>Einheitlich je Datei.</b> Anlage 1, Abschnitt 7.3: „Innerhalb einer
 *       Datei duerfen nicht verschiedene Verarbeitungskennzeichen genutzt
 *       werden. Je Verarbeitungskennzeichen ist eine eigene Datei zu
 *       uebermitteln."</li>
 *   <li><b>Korrekturen brauchen die Ursprungsangaben.</b> Bei einem
 *       Kennzeichen ungleich {@code 01} ist das {@code URI}-Segment mit
 *       Leistungserbringer-IK, Sammel- und Einzelrechnungsnummer,
 *       Rechnungsdatum und Belegnummer der Ursprungsrechnung zu uebermitteln -
 *       sonst kann die Kasse die Korrektur nicht zuordnen.</li>
 * </ol>
 */
public final class VerarbeitungskennzeichenRegel implements ValidationRule {

    private static final String FKT = "FKT";

    /** Die Ursprungsrechnungsinformationen, Anlage 1 Abschnitt 7.3. */
    private static final String URI = "URI";

    /** Das Verarbeitungskennzeichen steht an erster Stelle des FKT. */
    private static final int FKT_KENNZEICHEN = 0;

    /** Abrechnung ohne Besonderheiten - der Regelfall ohne Ursprungsbezug. */
    private static final String OHNE_BESONDERHEITEN = "01";

    /** Die Auspraegungen aus Anlage 3, Abschnitt 8.1.7. */
    private static final Set<String> ERLAUBT = Set.of("01", "02", "03", "04", "10");

    /** Wofuer die Werte stehen - fuer Meldungen, die etwas sagen. */
    private static String bedeutung(String kennzeichen) {
        return switch (kennzeichen) {
            case "01" -> "Abrechnung ohne Besonderheiten";
            case "02" -> "Nachforderung";
            case "03" -> "Zuzahlungsforderung";
            case "04" -> "Korrekturrechnung";
            case "10" -> "Wiederaufnahme";
            default -> "unbekannt";
        };
    }

    @Override
    public String getName() {
        return "Verarbeitungskennzeichen";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        List<DtaSegment> fkts = document.mitTag(FKT);
        if (fkts.isEmpty()) {
            return;
        }

        pruefeWerte(fkts, bericht);
        pruefeEinheitlichkeit(fkts, bericht);
        pruefeUrsprungsangaben(document, fkts, bericht);
    }

    /** Jeder Wert muss im Schluesselverzeichnis stehen. */
    private static void pruefeWerte(List<DtaSegment> fkts, ValidationReport.Builder bericht) {
        for (DtaSegment fkt : fkts) {
            String kennzeichen = fkt.element(FKT_KENNZEICHEN).trim();
            if (kennzeichen.isEmpty()) {
                bericht.error("FKT_KENNZEICHEN_FEHLT", fkt.ort(),
                        "Im FKT-Segment fehlt das Verarbeitungskennzeichen. Es ist eine"
                                + " Pflichtangabe (Anlage 3, Abschnitt 8.1.7).");
            } else if (!ERLAUBT.contains(kennzeichen)) {
                bericht.error("FKT_KENNZEICHEN_UNBEKANNT", fkt.ort(),
                        "Das Verarbeitungskennzeichen '" + kennzeichen + "' gibt es nicht."
                                + " Zulaessig sind 01 (Abrechnung ohne Besonderheiten),"
                                + " 02 (Nachforderung), 03 (Zuzahlungsforderung),"
                                + " 04 (Korrekturrechnung) und 10 (Wiederaufnahme)"
                                + " - Anlage 3, Abschnitt 8.1.7.");
            }
        }
    }

    /**
     * Eine Datei traegt genau ein Verarbeitungskennzeichen.
     *
     * <p>Die beiden Nachrichten einer Lieferung - Gesamtaufstellung und
     * Leistungsaufstellung - fuehren jeweils ein {@code FKT}, und die beiden
     * muessen uebereinstimmen. Faellt das auseinander, ordnet die Kasse die
     * Leistungsdaten der falschen Rechnungsart zu.</p>
     */
    private static void pruefeEinheitlichkeit(List<DtaSegment> fkts, ValidationReport.Builder bericht) {
        String erstes = fkts.get(0).element(FKT_KENNZEICHEN).trim();
        for (DtaSegment fkt : fkts) {
            String kennzeichen = fkt.element(FKT_KENNZEICHEN).trim();
            if (!kennzeichen.equals(erstes)) {
                bericht.error("FKT_KENNZEICHEN_UNEINHEITLICH", fkt.ort(),
                        "Diese Datei fuehrt zwei verschiedene Verarbeitungskennzeichen: '"
                                + erstes + "' (" + bedeutung(erstes) + ") und '" + kennzeichen
                                + "' (" + bedeutung(kennzeichen) + "). Je Verarbeitungskennzeichen"
                                + " ist eine eigene Datei zu uebermitteln"
                                + " (Anlage 1, Abschnitt 7.3).");
                return;
            }
        }
    }

    /**
     * Eine Korrektur ohne Ursprungsangaben laesst sich nicht zuordnen.
     *
     * <p>Das {@code URI}-Segment erzeugt das Programm heute nicht - es braucht
     * die Nummern der Ursprungsrechnung, und die haelt es bisher nirgends fest.
     * Wer ein Kennzeichen ungleich {@code 01} setzt, ohne das nachzuruesten,
     * bekommt hier die Beanstandung statt sie von der Kasse.</p>
     */
    private static void pruefeUrsprungsangaben(DtaDocument document, List<DtaSegment> fkts,
            ValidationReport.Builder bericht) {
        String kennzeichen = fkts.get(0).element(FKT_KENNZEICHEN).trim();
        if (kennzeichen.isEmpty() || OHNE_BESONDERHEITEN.equals(kennzeichen)
                || !ERLAUBT.contains(kennzeichen)) {
            return;
        }
        if (document.mitTag(URI).isEmpty()) {
            bericht.error("URI_FEHLT", fkts.get(0).ort(),
                    "Das Verarbeitungskennzeichen '" + kennzeichen + "' (" + bedeutung(kennzeichen)
                            + ") verlangt das URI-Segment mit den Angaben der Ursprungsrechnung:"
                            + " Leistungserbringer-IK, Sammel- und Einzelrechnungsnummer,"
                            + " Rechnungsdatum und Belegnummer. Ohne sie kann die Kasse die"
                            + " Korrektur nicht zuordnen (Anlage 1, Abschnitt 7.3).");
        }
    }
}
