package de.gkvtransmitter.validator.rules;

import java.util.ArrayList;
import java.util.List;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft jede einzelne Nachricht zwischen {@code UNH} und {@code UNT}.
 *
 * <p>Jede Nachricht traegt im {@code UNH} eine laufende Nummer. Das
 * zugehoerige {@code UNT} muss dieselbe Nummer wiederholen und ausserdem
 * angeben, aus wie vielen Segmenten die Nachricht besteht - {@code UNH} und
 * {@code UNT} eingeschlossen.</p>
 *
 * <p>Genau diese Zaehlung ist erfahrungsgemaess fehleranfaellig, weil sie sich
 * bei jeder Aenderung an der Nachricht mitverschiebt. Stimmt sie nicht, weist
 * die Kasse die Nachricht wegen Syntaxfehlers ab.</p>
 */
public final class NachrichtenAbschlussRegel implements ValidationRule {

    private static final String UNH = "UNH";
    private static final String UNT = "UNT";

    /** Die Nachrichtennummer steht im UNH an erster Stelle. */
    private static final int UNH_NUMMER = 0;
    /** Die Segmentanzahl steht im UNT an erster Stelle. */
    private static final int UNT_ANZAHL = 0;
    /** Die Nachrichtennummer steht im UNT an zweiter Stelle. */
    private static final int UNT_NUMMER = 1;

    @Override
    public String getName() {
        return "Nachrichtenabschluss (UNH/UNT)";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        List<DtaSegment> segmente = document.getSegments();
        List<DtaSegment> offeneKoepfe = new ArrayList<>();
        int segmenteSeitKopf = 0;

        for (DtaSegment segment : segmente) {
            if (segment.tag().equals(UNH)) {
                if (!offeneKoepfe.isEmpty()) {
                    bericht.fehler("NACHRICHT_NICHT_GESCHLOSSEN", segment.ort(),
                            offeneKoepfe.get(offeneKoepfe.size() - 1).element(UNH_NUMMER));
                    offeneKoepfe.clear();
                }
                offeneKoepfe.add(segment);
                segmenteSeitKopf = 1;
                continue;
            }

            if (!offeneKoepfe.isEmpty()) {
                segmenteSeitKopf++;
            }

            if (segment.tag().equals(UNT)) {
                if (offeneKoepfe.isEmpty()) {
                    bericht.fehler("UNT_OHNE_UNH", segment.ort());
                    continue;
                }
                pruefeAbschluss(offeneKoepfe.get(0), segment, segmenteSeitKopf, bericht);
                offeneKoepfe.clear();
                segmenteSeitKopf = 0;
            }
        }

        for (DtaSegment offen : offeneKoepfe) {
            bericht.fehler("UNH_OHNE_UNT", offen.ort(), offen.element(UNH_NUMMER));
        }
    }

    private void pruefeAbschluss(DtaSegment unh, DtaSegment unt, int gezaehlt, ValidationReport.Builder bericht) {
        String nummerKopf = unh.element(UNH_NUMMER).trim();
        String nummerFuss = unt.element(UNT_NUMMER).trim();

        if (!nummerKopf.equals(nummerFuss)) {
            bericht.fehler("NACHRICHT_NUMMER", unt.ort(), nummerFuss, nummerKopf);
        }

        String angabe = unt.element(UNT_ANZAHL).trim();
        if (angabe.isEmpty()) {
            bericht.fehler("UNT_ANZAHL_FEHLT", unt.ort());
            return;
        }
        try {
            int angegeben = Integer.parseInt(angabe);
            if (angegeben != gezaehlt) {
                bericht.fehler("UNT_ANZAHL", unt.ort(), angegeben, gezaehlt);
            }
        } catch (NumberFormatException e) {
            bericht.fehler("UNT_ANZAHL_UNGUELTIG", unt.ort(), angabe);
        }
    }
}
