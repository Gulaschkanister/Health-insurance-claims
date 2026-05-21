package de.gkvtransmitter.validator;

import de.gkvtransmitter.enums.InputOption;

/**
 * Beschreibt eine austauschbare Validierungsregel für Formularfelder.
 */
@FunctionalInterface
public interface ValidationRule {

    /**
     * Führt die Regelvalidierung aus und liefert ein Ergebnis.
     */
    ValidationResult validate(String fieldName, String fieldValue, InputOption inputOption, String fieldJavaType);
}
