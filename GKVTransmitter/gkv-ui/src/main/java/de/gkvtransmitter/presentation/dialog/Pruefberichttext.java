package de.gkvtransmitter.presentation.dialog;

import de.gkvtransmitter.validator.ValidationMessage;
import de.gkvtransmitter.validator.ValidationReport;

/** Bereitet einen Pruefbericht als Text fuer die Anzeige auf. */
public final class Pruefberichttext {

    /** Ueberschrift des Dialogs. */
    public static final String TITEL = "Pruefung nicht bestanden";

    private Pruefberichttext() {
    }

    /**
     * Setzt den Meldungstext zusammen.
     *
     * <p>Fehler und Warnungen stehen getrennt, weil sie unterschiedliche
     * Bedeutung haben: Fehler haben den Versand aufgehalten, Warnungen sind
     * Hinweise. Ohne die Trennung waere aus der Liste nicht ersichtlich, was
     * behoben werden muss.</p>
     */
    public static String formatiere(ValidationReport bericht) {
        StringBuilder text = new StringBuilder();
        text.append("Die Abrechnung wurde nicht versendet.").append(System.lineSeparator());
        text.append(System.lineSeparator());

        if (!bericht.getErrors().isEmpty()) {
            text.append("Zu beheben:").append(System.lineSeparator());
            for (ValidationMessage befund : bericht.getErrors()) {
                text.append("  - ").append(befund.text());
                if (!befund.ort().isEmpty()) {
                    text.append("  [").append(befund.ort()).append(']');
                }
                text.append(System.lineSeparator());
            }
        }
        if (!bericht.getWarnings().isEmpty()) {
            text.append(System.lineSeparator()).append("Hinweise:").append(System.lineSeparator());
            for (ValidationMessage befund : bericht.getWarnings()) {
                text.append("  - ").append(befund.text()).append(System.lineSeparator());
            }
        }
        return text.toString();
    }
}
