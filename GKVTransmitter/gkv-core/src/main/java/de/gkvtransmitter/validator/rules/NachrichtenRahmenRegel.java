package de.gkvtransmitter.validator.rules;

import java.util.List;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft die aeussere Klammer der Uebertragung.
 *
 * <p>Eine Lieferung beginnt mit genau einem {@code UNB} und endet mit genau
 * einem {@code UNZ}. Das {@code UNZ} nennt zwei Angaben, die beide stimmen
 * muessen: die Anzahl der enthaltenen Nachrichten und die
 * Datenaustauschreferenz aus dem {@code UNB}.</p>
 *
 * <p>Die Anzahl war in der Erzeugung fest auf 2 verdrahtet. Solange genau eine
 * SLGA- und eine SLLA-Nachricht entstanden, ist das nicht aufgefallen - bei
 * jeder Abweichung haette die Kasse die Lieferung abgewiesen.</p>
 */
public final class NachrichtenRahmenRegel implements ValidationRule {

    private static final String UNB = "UNB";
    private static final String UNZ = "UNZ";
    private static final String UNH = "UNH";

    /** Im UNZ steht die Nachrichtenanzahl an erster Stelle. */
    private static final int UNZ_ANZAHL = 0;
    /** Im UNZ steht die Datenaustauschreferenz an zweiter Stelle. */
    private static final int UNZ_REFERENZ = 1;
    /**
     * Im UNB steht die Datenaustauschreferenz an fuenfter Stelle.
     *
     * <p>Aufbau: {@code UNB+UNOC:3+Absender+Empfaenger+Datum:Zeit+Referenz+...},
     * die Zaehlung beginnt hinter dem Bezeichner bei 0.</p>
     */
    private static final int UNB_REFERENZ = 4;

    @Override
    public String getName() {
        return "Nachrichtenrahmen (UNB/UNZ)";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        List<DtaSegment> unbSegmente = document.mitTag(UNB);
        List<DtaSegment> unzSegmente = document.mitTag(UNZ);

        if (unbSegmente.isEmpty()) {
            bericht.fehler("RAHMEN_UNB_FEHLT", "");
        } else if (unbSegmente.size() > 1) {
            bericht.fehler("RAHMEN_UNB_MEHRFACH", unbSegmente.get(1).ort(), unbSegmente.size());
        }

        if (unzSegmente.isEmpty()) {
            bericht.fehler("RAHMEN_UNZ_FEHLT", "");
            return;
        }
        if (unzSegmente.size() > 1) {
            bericht.fehler("RAHMEN_UNZ_MEHRFACH", unzSegmente.get(1).ort(), unzSegmente.size());
        }

        DtaSegment unz = unzSegmente.get(unzSegmente.size() - 1);
        pruefeReihenfolge(document, unz, bericht);
        pruefeAnzahl(document, unz, bericht);

        if (!unbSegmente.isEmpty()) {
            pruefeReferenz(unbSegmente.get(0), unz, bericht);
        }
    }

    private void pruefeReihenfolge(DtaDocument document, DtaSegment unz, ValidationReport.Builder bericht) {
        List<DtaSegment> alle = document.getSegments();
        if (!alle.isEmpty() && !alle.get(0).tag().equals(UNB)) {
            bericht.fehler("RAHMEN_UNB_NICHT_ZUERST", alle.get(0).ort(), alle.get(0).tag());
        }
        if (!alle.isEmpty() && !alle.get(alle.size() - 1).equals(unz)) {
            bericht.fehler("RAHMEN_UNZ_NICHT_ZULETZT", unz.ort());
        }
    }

    private void pruefeAnzahl(DtaDocument document, DtaSegment unz, ValidationReport.Builder bericht) {
        String angabe = unz.element(UNZ_ANZAHL);
        int tatsaechlich = document.anzahlMitTag(UNH);

        if (angabe.isBlank()) {
            bericht.fehler("UNZ_ANZAHL_FEHLT", unz.ort());
            return;
        }
        try {
            int angegeben = Integer.parseInt(angabe.trim());
            if (angegeben != tatsaechlich) {
                bericht.fehler("UNZ_ANZAHL", unz.ort(), angegeben, tatsaechlich);
            }
        } catch (NumberFormatException e) {
            bericht.fehler("UNZ_ANZAHL_UNGUELTIG", unz.ort(), angabe);
        }
    }

    private void pruefeReferenz(DtaSegment unb, DtaSegment unz, ValidationReport.Builder bericht) {
        String imKopf = unb.element(UNB_REFERENZ).trim();
        String imFuss = unz.element(UNZ_REFERENZ).trim();

        if (imKopf.isEmpty()) {
            bericht.fehler("UNB_REFERENZ_FEHLT", unb.ort());
            return;
        }
        if (!imKopf.equals(imFuss)) {
            bericht.fehler("RAHMEN_REFERENZ", unz.ort(), imFuss, imKopf);
        }
    }
}
