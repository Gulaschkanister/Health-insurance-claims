package de.gkvtransmitter.validator.rules;

import java.util.Set;
import java.util.regex.Pattern;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft den formalen Aufbau der einzelnen Segmente.
 *
 * <p>Geprueft wird, was sich ohne fachliche Kenntnis der Nachricht feststellen
 * laesst: dass jedes Segment mit {@code '} abgeschlossen ist, dass der
 * Bezeichner aus drei Grossbuchstaben besteht, und dass er zu den im Verfahren
 * vorgesehenen gehoert.</p>
 *
 * <p>Ein unbekannter Bezeichner ist eine Warnung und kein Fehler: die Liste
 * bildet den heute unterstuetzten Umfang ab, nicht den gesamten Standard. Ein
 * Segment, das hier noch fehlt, soll den Versand nicht aufhalten - aber
 * auffallen.</p>
 */
public final class SegmentSyntaxRegel implements ValidationRule {

    private static final Pattern BEZEICHNER = Pattern.compile("^[A-Z]{3}$");

    /**
     * Die im Verfahren verwendeten Segmentbezeichner.
     *
     * <p>Deckungsgleich mit den Definitionen unter
     * {@code src/main/resources/segments}.</p>
     */
    private static final Set<String> BEKANNTE_SEGMENTE = Set.of(
            "UNB", "UNH", "UNT", "UNZ",
            "FKT", "REC", "UST", "GES", "NAM",
            "INV", "NAD", "ENF", "BES");

    @Override
    public String getName() {
        return "Segmentsyntax";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        if (document.istLeer()) {
            bericht.fehler("SYNTAX_LEER", "");
            return;
        }

        for (DtaSegment segment : document.getSegments()) {
            pruefeAbschluss(segment, bericht);
            pruefeBezeichner(segment, bericht);
        }
    }

    private void pruefeAbschluss(DtaSegment segment, ValidationReport.Builder bericht) {
        if (!segment.raw().endsWith(String.valueOf(DtaDocument.SEGMENT_ENDE))) {
            bericht.fehler("SYNTAX_ABSCHLUSS", segment.ort(), DtaDocument.SEGMENT_ENDE);
        }
    }

    private void pruefeBezeichner(DtaSegment segment, ValidationReport.Builder bericht) {
        String tag = segment.tag();

        if (!BEZEICHNER.matcher(tag).matches()) {
            bericht.fehler("SYNTAX_BEZEICHNER", segment.ort(), tag);
            return;
        }
        if (!BEKANNTE_SEGMENTE.contains(tag)) {
            bericht.warnung("SYNTAX_UNBEKANNT", segment.ort(), tag);
        }
        if (segment.elementAnzahl() == 0) {
            bericht.warnung("SYNTAX_OHNE_INHALT", segment.ort(), tag);
        }
    }
}
