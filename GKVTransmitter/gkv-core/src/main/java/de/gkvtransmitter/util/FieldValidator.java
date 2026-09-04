package de.gkvtransmitter.util;

import de.gkvtransmitter.enums.InputOption;

/**
 * Generelle Validierungslogik für Formularfelder.
 * Erlaubt verschiedene Validierungen je nach InputOption und Feldtyp.
 */
public final class FieldValidator {

    private FieldValidator() {
    }

    /**
     * Validiert einen Feldwert basierend auf dem InputOption und Feldtyp.
     *
     * @param fieldName Name des Feldes (für Fehlermeldungen)
     * @param fieldValue Der zu validierende Wert
     * @param inputOption Der InputOption des Feldes
     * @param fieldJavaType Der Java-Typ des Feldes
     * @return Befund zu diesem Feld
     */
    public static Feldbefund validate(String fieldName, String fieldValue,
                                           InputOption inputOption, String fieldJavaType) {
        if (fieldValue == null || fieldValue.isBlank()) {
            return Feldbefund.EMPTY;
        }

        return switch (inputOption) {
            case NUMBER, COST, PERCENT -> validateNumericField(fieldName, fieldValue, fieldJavaType);
            case STRING -> validateStringField(fieldName, fieldValue);
            case DATE -> validateDateField(fieldName, fieldValue);
            case CODE -> validateCodeField(fieldName, fieldValue);
            case BOOLEAN -> Feldbefund.VALID;
            case NUMBER_SUGGESTION -> validateNumericField(fieldName, fieldValue, fieldJavaType);
            case TIME -> validateTimeField(fieldName, fieldValue);
            default -> Feldbefund.VALID;
        };
    }

    /**
     * Validiert ein numerisches Feld.
     */
    private static Feldbefund validateNumericField(String fieldName, String fieldValue, String fieldJavaType) {
        try {
            if ("int".equalsIgnoreCase(fieldJavaType) || "Integer".equalsIgnoreCase(fieldJavaType)) {
                Integer.valueOf(fieldValue);
            } else if ("BigDecimal".equalsIgnoreCase(fieldJavaType)) {
                java.math.BigDecimal parsedValue = new java.math.BigDecimal(fieldValue);
                parsedValue.toPlainString();
            } else if ("double".equalsIgnoreCase(fieldJavaType)) {
                Double.valueOf(fieldValue);
            } else if ("long".equalsIgnoreCase(fieldJavaType)) {
                Long.valueOf(fieldValue);
            }
            return Feldbefund.VALID;
        } catch (NumberFormatException e) {
            return new Feldbefund(false,
                String.format("%s muss eine gültige Zahl sein", fieldName));
        }
    }

    /**
     * Validiert ein String-Feld.
     */
    private static Feldbefund validateStringField(String fieldName, String fieldValue) {
        if (fieldValue.length() > 1000) {
            return new Feldbefund(false,
                String.format("%s ist zu lang (max. 1000 Zeichen)", fieldName));
        }
        return Feldbefund.VALID;
    }

    /**
     * Validiert ein Datumfeld (erwartet ISO-Format: YYYY-MM-DD).
     */
    private static Feldbefund validateDateField(String fieldName, String fieldValue) {
        try {
            java.time.LocalDate.parse(fieldValue);
            return Feldbefund.VALID;
        } catch (java.time.format.DateTimeParseException e) {
            return new Feldbefund(false,
                String.format("%s muss im Format YYYY-MM-DD sein", fieldName));
        }
    }

    /**
     * Validiert ein Zeitfeld (erwartet Format: HH:MM:SS).
     */
    private static Feldbefund validateTimeField(String fieldName, String fieldValue) {
        if (!fieldValue.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return new Feldbefund(false,
                String.format("%s muss im Format HH:MM:SS sein", fieldName));
        }
        return Feldbefund.VALID;
    }

    /**
     * Validiert ein Code-Feld (alphanumerisch mit optionalen Bindestrichen).
     */
    private static Feldbefund validateCodeField(String fieldName, String fieldValue) {
        if (!fieldValue.matches("[A-Za-z0-9\\-]+")) {
            return new Feldbefund(false,
                String.format("%s darf nur Buchstaben, Zahlen und Bindestriche enthalten", fieldName));
        }
        return Feldbefund.VALID;
    }

    /**
     * Ergebnis der Pruefung eines einzelnen Formularfelds.
     *
     * <p>Hiess zuvor {@code ValidationResult} und war damit kaum von
     * {@code validator.ValidationReport} zu unterscheiden, obwohl beide
     * nichts miteinander zu tun haben: dieser Befund betrifft eine Eingabe in
     * der Oberflaeche, jener Bericht die fertige DTA-Nachricht. Der neue Name
     * sagt, worum es geht - um ein Feld.</p>
     */
    public static class Feldbefund {
        public static final Feldbefund VALID = new Feldbefund(true, null);
        public static final Feldbefund EMPTY = new Feldbefund(true, null);

        public final boolean isValid;
        public final String errorMessage;

        public Feldbefund(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
}
