package de.gkvtransmitter.validator;

import java.util.Objects;

/**
 * Ein einzelner Pruefbefund.
 *
 * @param severity Gewicht des Befunds
 * @param code     stabiler Schluessel der Regel, etwa {@code UNZ_ANZAHL}.
 *                 Bewusst getrennt vom Text, damit sich Befunde auswerten und
 *                 filtern lassen, ohne Meldungstexte zu vergleichen.
 * @param ort      Fundstelle, etwa {@code UNZ (Zeile 18)}
 * @param text     Beschreibung in ganzen Saetzen, an Anwender gerichtet
 */
public record ValidationMessage(ValidationSeverity severity, String code, String ort, String text) {

    public ValidationMessage {
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(text, "text must not be null");
        ort = ort == null ? "" : ort;
    }

    public static ValidationMessage error(String code, String ort, String text) {
        return new ValidationMessage(ValidationSeverity.ERROR, code, ort, text);
    }

    public static ValidationMessage warning(String code, String ort, String text) {
        return new ValidationMessage(ValidationSeverity.WARNING, code, ort, text);
    }

    public static ValidationMessage info(String code, String ort, String text) {
        return new ValidationMessage(ValidationSeverity.INFO, code, ort, text);
    }

    @Override
    public String toString() {
        String stelle = ort.isEmpty() ? "" : " [" + ort + "]";
        return severity + " " + code + stelle + ": " + text;
    }
}
