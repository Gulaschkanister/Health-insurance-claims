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
            bericht.fehler("VERSICHERTER_NACHNAME", nad.ort());
        }
        if (nad.element(NAD_VORNAME).trim().isEmpty()) {
            bericht.fehler("VERSICHERTER_VORNAME", nad.ort());
        }
    }

    /** " von ANNA BERGER", oder leer, wenn im NAD kein Name steht. */
    private static String wessen(DtaSegment nad) {
        String name = (nad.element(NAD_VORNAME).trim() + " " + nad.element(NAD_NACHNAME).trim()).trim();
        return name.isEmpty() ? "" : " von " + name;
    }

    private void pruefeGeburtsdatum(DtaSegment nad, ValidationReport.Builder bericht) {
        String roh = nad.element(NAD_GEBURTSDATUM).trim();
        if (roh.isEmpty()) {
            // Mit Namen: seit der Ersatzwert in DtaFactory weg ist, haelt diese
            // Meldung wirklich einen Lauf auf - dann muss sie auch sagen, bei
            // wem nachzutragen ist. Eine Zeilennummer allein hilft niemandem,
            // der zwanzig Teilnehmerinnen abrechnet.
            bericht.fehler("VERSICHERTER_GEBURTSDATUM", nad.ort(), wessen(nad));
            return;
        }
        try {
            LocalDate geburtsdatum = LocalDate.parse(roh, DATUM);
            LocalDate heute = LocalDate.now();

            if (geburtsdatum.isAfter(heute)) {
                bericht.fehler("GEBURTSDATUM_ZUKUNFT", nad.ort(), roh);
            } else if (geburtsdatum.isBefore(heute.minusYears(AUFFAELLIGES_ALTER))) {
                bericht.warnung("GEBURTSDATUM_ALT", nad.ort(), roh, AUFFAELLIGES_ALTER);
            }
        } catch (DateTimeParseException e) {
            bericht.fehler("GEBURTSDATUM_FORMAT", nad.ort(), roh);
        }
    }

    private void pruefeVersichertennummer(DtaSegment inv, ValidationReport.Builder bericht) {
        String nummer = inv.komponente(INV_VERSICHERTENNUMMER, 0).trim();
        if (nummer.isEmpty()) {
            bericht.fehler("VERSICHERTENNUMMER_FEHLT", inv.ort());
            return;
        }
        if (nummer.chars().allMatch(z -> z == '0')) {
            bericht.fehler("VERSICHERTENNUMMER_NULL", inv.ort());
        }
    }
}
