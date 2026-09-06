package de.gkvtransmitter.validator.rules;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.util.Institutionskennzeichen;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft die Institutionskennzeichen von Absender und Empfaenger.
 *
 * <p>Im {@code UNB} stehen die IK der beiden Beteiligten, im {@code FKT}
 * zusaetzlich die Rollen innerhalb der Abrechnung. Ein IK mit falscher
 * Pruefziffer fuehrt dazu, dass die Kasse die gesamte Lieferung zurueckweist -
 * und zwar ohne fachliche Pruefung des Inhalts.</p>
 *
 * <p>Die Regel meldet einen Fehler nur bei nachweislich falscher Pruefziffer
 * oder falscher Laenge. Dass ein formal gueltiges IK auch tatsaechlich vergeben
 * ist, laesst sich hier nicht feststellen.</p>
 */
public final class InstitutionskennzeichenRegel implements ValidationRule {

    private static final String UNB = "UNB";

    /** Absender-IK, zweite Stelle im UNB. */
    private static final int UNB_ABSENDER = 1;
    /** Empfaenger-IK, dritte Stelle im UNB. */
    private static final int UNB_EMPFAENGER = 2;

    @Override
    public String getName() {
        return "Institutionskennzeichen";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        document.erstesMitTag(UNB).ifPresent(unb -> {
            pruefeKennzeichen(unb, UNB_ABSENDER, "Absender", bericht);
            pruefeKennzeichen(unb, UNB_EMPFAENGER, "Empfaenger", bericht);
        });
    }

    private void pruefeKennzeichen(DtaSegment unb, int position, String rolle, ValidationReport.Builder bericht) {
        // Das IK kann mit Qualifier notiert sein, etwa "108310400:01".
        String wert = unb.komponente(position, 0).trim();

        if (wert.isEmpty()) {
            bericht.error("IK_FEHLT", unb.ort(),
                    "Im UNB-Segment fehlt das Institutionskennzeichen des " + rolle + "s.");
            return;
        }
        if (wert.length() != Institutionskennzeichen.LAENGE) {
            bericht.error("IK_LAENGE", unb.ort(),
                    "Das %s-IK '%s' hat %d Stellen, erwartet werden %d."
                            .formatted(rolle, wert, wert.length(), Institutionskennzeichen.LAENGE));
            return;
        }
        if (!wert.chars().allMatch(Character::isDigit)) {
            bericht.error("IK_ZIFFERN", unb.ort(),
                    "Das %s-IK '%s' enthaelt Zeichen, die keine Ziffern sind.".formatted(rolle, wert));
            return;
        }
        if (!Institutionskennzeichen.istGueltig(wert)) {
            bericht.error("IK_PRUEFZIFFER", unb.ort(),
                    "Das %s-IK '%s' hat eine falsche Pruefziffer, erwartet wurde %d an letzter Stelle."
                            .formatted(rolle, wert, Institutionskennzeichen.berechnePruefziffer(wert)));
        }
    }
}
