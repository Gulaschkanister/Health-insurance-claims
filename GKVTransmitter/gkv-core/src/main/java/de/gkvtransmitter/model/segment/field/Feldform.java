package de.gkvtransmitter.model.segment.field;

import de.gkvtransmitter.enums.InputOption;

/**
 * Was fuer ein Wert in ein Feld gehoert.
 *
 * <p>Vier Angaben, die nur zusammen einen Sinn ergeben: Ein
 * {@code maxLength} ohne {@code type} sagt nichts, und ein {@code inputType}
 * ohne {@code isMandatory} auch nicht. Sie standen einzeln in einem
 * Konstruktor mit neun Parametern.</p>
 *
 * @param type        der Datentyp aus der Segmentdefinition
 * @param isMandatory ob die Anlage das Feld als Pflicht fuehrt
 * @param maxLength   Hoechstlaenge, {@code 0} fuer unbegrenzt
 * @param inputType   wie das Feld im Formular auszusehen hat
 */
public record Feldform(FieldType type, boolean isMandatory, int maxLength, InputOption inputType) {
}
