package de.gkvtransmitter.util;

import java.util.List;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.validator.ValidationResult;
import de.gkvtransmitter.validator.ValidationRule;

/**
 * Generelle Validierungslogik für Formularfelder.
 * Erlaubt verschiedene Validierungen je nach InputOption und Feldtyp.
 */
public class FieldValidator {

    private static final List<ValidationRule> RULES = List.of(
            FieldValidator::validateNumericRule,
            FieldValidator::validateStringRule,
            FieldValidator::validateDateRule,
            FieldValidator::validateTimeRule,
            FieldValidator::validateCodeRule);

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

        for (ValidationRule rule : RULES) {
            ValidationResult result = rule.validate(fieldName, fieldValue, inputOption, fieldJavaType);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.VALID;
    }

    private static ValidationResult validateNumericRule(String fieldName, String fieldValue, InputOption inputOption,
            String fieldJavaType) {
        if (inputOption != InputOption.NUMBER
                && inputOption != InputOption.COST
                && inputOption != InputOption.PERCENT
                && inputOption != InputOption.NUMBER_SUGGESTION) {
            return ValidationResult.VALID;
        }

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
            } else {
                new java.math.BigDecimal(fieldValue);
            }
            return ValidationResult.VALID;
        } catch (NumberFormatException e) {
            return ValidationResult.invalid(String.format("%s muss eine gültige Zahl sein", fieldName));
        }
    }

    private static ValidationResult validateStringRule(String fieldName, String fieldValue, InputOption inputOption,
            String fieldJavaType) {
        if (inputOption != InputOption.STRING) {
            return ValidationResult.VALID;
        }

        if (fieldValue.length() > 1000) {
            return ValidationResult.invalid(String.format("%s ist zu lang (max. 1000 Zeichen)", fieldName));
        }
        return ValidationResult.VALID;
    }

    private static ValidationResult validateDateRule(String fieldName, String fieldValue, InputOption inputOption,
            String fieldJavaType) {
        if (inputOption != InputOption.DATE) {
            return ValidationResult.VALID;
        }

        try {
            java.time.LocalDate.parse(fieldValue);
            return ValidationResult.VALID;
        } catch (java.time.format.DateTimeParseException e) {
            return ValidationResult.invalid(String.format("%s muss im Format YYYY-MM-DD sein", fieldName));
        }
    }

    private static ValidationResult validateTimeRule(String fieldName, String fieldValue, InputOption inputOption,
            String fieldJavaType) {
        if (inputOption != InputOption.TIME) {
            return ValidationResult.VALID;
        }

        if (!fieldValue.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return ValidationResult.invalid(String.format("%s muss im Format HH:MM:SS sein", fieldName));
        }
        return ValidationResult.VALID;
    }

    private static ValidationResult validateCodeRule(String fieldName, String fieldValue, InputOption inputOption,
            String fieldJavaType) {
        if (inputOption != InputOption.CODE) {
            return ValidationResult.VALID;
        }

        if (!fieldValue.matches("[A-Za-z0-9\\-]+")) {
            return ValidationResult.invalid(
                    String.format("%s darf nur Buchstaben, Zahlen und Bindestriche enthalten", fieldName));
        }
        return ValidationResult.VALID;
    }
}
