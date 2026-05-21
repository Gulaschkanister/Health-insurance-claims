package de.gkvtransmitter.validator;

/**
 * Ergebnis einer Validierung mit Status und optionaler Fehlermeldung.
 */
public final class ValidationResult {

    public static final ValidationResult VALID = new ValidationResult(true, null);
    public static final ValidationResult EMPTY = new ValidationResult(true, null);

    private final boolean valid;
    private final String errorMessage;

    public ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
