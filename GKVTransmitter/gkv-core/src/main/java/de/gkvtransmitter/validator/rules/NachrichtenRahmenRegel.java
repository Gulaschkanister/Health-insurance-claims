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
            bericht.error("RAHMEN_UNB_FEHLT", "",
                    "Die Uebertragung enthaelt kein UNB-Segment und hat damit keinen Kopf.");
        } else if (unbSegmente.size() > 1) {
            bericht.error("RAHMEN_UNB_MEHRFACH", unbSegmente.get(1).ort(),
                    "Eine Uebertragung darf nur ein UNB-Segment enthalten, gefunden: " + unbSegmente.size() + ".");
        }

        if (unzSegmente.isEmpty()) {
            bericht.error("RAHMEN_UNZ_FEHLT", "",
                    "Die Uebertragung enthaelt kein UNZ-Segment und ist damit nicht abgeschlossen.");
            return;
        }
        if (unzSegmente.size() > 1) {
            bericht.error("RAHMEN_UNZ_MEHRFACH", unzSegmente.get(1).ort(),
                    "Eine Uebertragung darf nur ein UNZ-Segment enthalten, gefunden: " + unzSegmente.size() + ".");
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
            bericht.error("RAHMEN_UNB_NICHT_ZUERST", alle.get(0).ort(),
                    "Die Uebertragung muss mit UNB beginnen, gefunden wurde " + alle.get(0).tag() + ".");
        }
        if (!alle.isEmpty() && !alle.get(alle.size() - 1).equals(unz)) {
            bericht.error("RAHMEN_UNZ_NICHT_ZULETZT", unz.ort(),
                    "Das UNZ-Segment muss das letzte Segment der Uebertragung sein.");
        }
    }

    private void pruefeAnzahl(DtaDocument document, DtaSegment unz, ValidationReport.Builder bericht) {
        String angabe = unz.element(UNZ_ANZAHL);
        int tatsaechlich = document.anzahlMitTag(UNH);

        if (angabe.isBlank()) {
            bericht.error("UNZ_ANZAHL_FEHLT", unz.ort(),
                    "Im UNZ-Segment fehlt die Anzahl der enthaltenen Nachrichten.");
            return;
        }
        try {
            int angegeben = Integer.parseInt(angabe.trim());
            if (angegeben != tatsaechlich) {
                bericht.error("UNZ_ANZAHL", unz.ort(),
                        "Das UNZ-Segment nennt %d Nachrichten, enthalten sind aber %d."
                                .formatted(angegeben, tatsaechlich));
            }
        } catch (NumberFormatException e) {
            bericht.error("UNZ_ANZAHL_UNGUELTIG", unz.ort(),
                    "Die Nachrichtenanzahl im UNZ-Segment ist keine Zahl: " + angabe + ".");
        }
    }

    private void pruefeReferenz(DtaSegment unb, DtaSegment unz, ValidationReport.Builder bericht) {
        String imKopf = unb.element(UNB_REFERENZ).trim();
        String imFuss = unz.element(UNZ_REFERENZ).trim();

        if (imKopf.isEmpty()) {
            bericht.error("UNB_REFERENZ_FEHLT", unb.ort(),
                    "Im UNB-Segment fehlt die Datenaustauschreferenz.");
            return;
        }
        if (!imKopf.equals(imFuss)) {
            bericht.error("RAHMEN_REFERENZ", unz.ort(),
                    "Die Datenaustauschreferenz im UNZ (%s) weicht von der im UNB (%s) ab."
                            .formatted(imFuss, imKopf));
        }
    }
}
