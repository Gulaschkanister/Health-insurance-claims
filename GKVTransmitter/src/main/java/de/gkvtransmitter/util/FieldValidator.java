package de.gkvtransmitter.util;

import de.gkvtransmitter.enums.InputOption;

/**
 * Generelle Validierungslogik für Formularfelder.
 * Erlaubt verschiedene Validierungen je nach InputOption und Feldtyp.
 */
public class FieldValidator {

    /**
     * Validiert einen Feldwert basierend auf dem InputOption und Feldtyp.
     * 
     * @param fieldName Name des Feldes (für Fehlermeldungen)
     * @param fieldValue Der zu validierende Wert
     * @param inputOption Der InputOption des Feldes
     * @param fieldJavaType Der Java-Typ des Feldes
     * @return ValidationResult mit Details zum Validierungsergebnis
     */
    public static ValidationResult validate(String fieldName, String fieldValue, 
                                           InputOption inputOption, String fieldJavaType) {
        if (fieldValue == null || fieldValue.isBlank()) {
            return ValidationResult.EMPTY;
        }

        return switch (inputOption) {
            case NUMBER, COST, PERCENT -> validateNumericField(fieldName, fieldValue, fieldJavaType);
            case STRING -> validateStringField(fieldName, fieldValue);
            case DATE -> validateDateField(fieldName, fieldValue);
            case CODE -> validateCodeField(fieldName, fieldValue);
            case BOOLEAN -> ValidationResult.VALID;
            case NUMBER_SUGGESTION -> validateNumericField(fieldName, fieldValue, fieldJavaType);
            case TIME -> validateTimeField(fieldName, fieldValue);
            default -> ValidationResult.VALID;
        };
    }

    /**
     * Validiert ein numerisches Feld.
     */
    private static ValidationResult validateNumericField(String fieldName, String fieldValue, String fieldJavaType) {
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
            return ValidationResult.VALID;
        } catch (NumberFormatException e) {
            return new ValidationResult(false, 
                String.format("%s muss eine gültige Zahl sein", fieldName));
        }
    }

    /**
     * Validiert ein String-Feld.
     */
    private static ValidationResult validateStringField(String fieldName, String fieldValue) {
        if (fieldValue.length() > 1000) {
            return new ValidationResult(false,
                String.format("%s ist zu lang (max. 1000 Zeichen)", fieldName));
        }
        return ValidationResult.VALID;
    }

    /**
     * Validiert ein Datumfeld (erwartet ISO-Format: YYYY-MM-DD).
     */
    private static ValidationResult validateDateField(String fieldName, String fieldValue) {
        try {
            java.time.LocalDate.parse(fieldValue);
            return ValidationResult.VALID;
        } catch (java.time.format.DateTimeParseException e) {
            return new ValidationResult(false,
                String.format("%s muss im Format YYYY-MM-DD sein", fieldName));
        }
    }

    /**
     * Validiert ein Zeitfeld (erwartet Format: HH:MM:SS).
     */
    private static ValidationResult validateTimeField(String fieldName, String fieldValue) {
        if (!fieldValue.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return new ValidationResult(false,
                String.format("%s muss im Format HH:MM:SS sein", fieldName));
        }
        return ValidationResult.VALID;
    }

    /**
     * Validiert ein Code-Feld (alphanumerisch mit optionalen Bindestrichen).
     */
    private static ValidationResult validateCodeField(String fieldName, String fieldValue) {
        if (!fieldValue.matches("[A-Za-z0-9\\-]+")) {
            return new ValidationResult(false,
                String.format("%s darf nur Buchstaben, Zahlen und Bindestriche enthalten", fieldName));
        }
        return ValidationResult.VALID;
    }

    /**
     * Ergebnis einer Feldvalidierung.
     */
    public static class ValidationResult {
        public static final ValidationResult VALID = new ValidationResult(true, null);
        public static final ValidationResult EMPTY = new ValidationResult(true, null);

        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
}
