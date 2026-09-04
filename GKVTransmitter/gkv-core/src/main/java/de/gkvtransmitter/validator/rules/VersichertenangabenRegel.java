package de.gkvtransmitter.validator.rules;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import de.gkvtransmitter.dta.DtaDocument;
import de.gkvtransmitter.dta.DtaSegment;
import de.gkvtransmitter.validator.ValidationReport;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Prueft die Angaben zur versicherten Person.
 *
 * <p>Name, Geburtsdatum und Versichertennummer entscheiden darueber, ob die
 * Kasse die Leistung einem Versicherungsverhaeltnis zuordnen kann. Fehlen sie
 * oder sind sie unplausibel, wird die Rechnung zurueckgewiesen, obwohl die
 * Leistung erbracht wurde.</p>
 *
 * <p>Ein Geburtsdatum in der Zukunft ist ein Fehler. Ein Alter jenseits von 120
 * Jahren ist eine Warnung: es ist auffaellig, aber nicht ausgeschlossen, und
 * deutet meist auf einen Zahlendreher hin.</p>
 */
public final class VersichertenangabenRegel implements ValidationRule {

    private static final String NAD = "NAD";
    private static final String INV = "INV";

    private static final int NAD_NACHNAME = 0;
    private static final int NAD_VORNAME = 1;
    private static final int NAD_GEBURTSDATUM = 2;
    private static final int INV_VERSICHERTENNUMMER = 0;

    private static final DateTimeFormatter DATUM = DateTimeFormatter.BASIC_ISO_DATE;

    /** Oberhalb dieses Alters ist ein Geburtsdatum erfahrungsgemaess ein Tippfehler. */
    private static final int AUFFAELLIGES_ALTER = 120;

    @Override
    public String getName() {
        return "Versichertenangaben";
    }

    @Override
    public void pruefe(DtaDocument document, ValidationReport.Builder bericht) {
        for (DtaSegment nad : document.mitTag(NAD)) {
            pruefeName(nad, bericht);
            pruefeGeburtsdatum(nad, bericht);
        }
        for (DtaSegment inv : document.mitTag(INV)) {
            pruefeVersichertennummer(inv, bericht);
        }
    }

    private void pruefeName(DtaSegment nad, ValidationReport.Builder bericht) {
        if (nad.element(NAD_NACHNAME).trim().isEmpty()) {
            bericht.error("VERSICHERTER_NACHNAME", nad.ort(),
                    "Im NAD-Segment fehlt der Nachname der versicherten Person.");
        }
        if (nad.element(NAD_VORNAME).trim().isEmpty()) {
            bericht.error("VERSICHERTER_VORNAME", nad.ort(),
                    "Im NAD-Segment fehlt der Vorname der versicherten Person.");
        }
    }

    private void pruefeGeburtsdatum(DtaSegment nad, ValidationReport.Builder bericht) {
        String roh = nad.element(NAD_GEBURTSDATUM).trim();
        if (roh.isEmpty()) {
            bericht.error("VERSICHERTER_GEBURTSDATUM", nad.ort(),
                    "Im NAD-Segment fehlt das Geburtsdatum.");
            return;
        }
        try {
            LocalDate geburtsdatum = LocalDate.parse(roh, DATUM);
            LocalDate heute = LocalDate.now();

            if (geburtsdatum.isAfter(heute)) {
                bericht.error("GEBURTSDATUM_ZUKUNFT", nad.ort(),
                        "Das Geburtsdatum %s liegt in der Zukunft.".formatted(roh));
            } else if (geburtsdatum.isBefore(heute.minusYears(AUFFAELLIGES_ALTER))) {
                bericht.warning("GEBURTSDATUM_ALT", nad.ort(),
                        "Das Geburtsdatum %s ergibt ein Alter ueber %d Jahren - bitte pruefen."
                                .formatted(roh, AUFFAELLIGES_ALTER));
            }
        } catch (DateTimeParseException e) {
            bericht.error("GEBURTSDATUM_FORMAT", nad.ort(),
                    "Das Geburtsdatum '%s' entspricht nicht dem Format JJJJMMTT.".formatted(roh));
        }
    }

    private void pruefeVersichertennummer(DtaSegment inv, ValidationReport.Builder bericht) {
        String nummer = inv.komponente(INV_VERSICHERTENNUMMER, 0).trim();
        if (nummer.isEmpty()) {
            bericht.error("VERSICHERTENNUMMER_FEHLT", inv.ort(),
                    "Im INV-Segment fehlt die Versichertennummer.");
            return;
        }
        if (nummer.chars().allMatch(z -> z == '0')) {
            bericht.error("VERSICHERTENNUMMER_NULL", inv.ort(),
                    "Die Versichertennummer besteht nur aus Nullen und kann keiner Person zugeordnet werden.");
        }
    }
}
