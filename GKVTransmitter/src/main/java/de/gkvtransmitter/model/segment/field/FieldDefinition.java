package de.gkvtransmitter.model.segment.field;

import de.gkvtransmitter.enums.InputOption;
import lombok.Getter;

/**
 * Diese Klasse soll spaeter die Regeln fuer ein einzelnes Feld wie Typ,
 * Pflichtstatus und Laengenbegrenzung beschreiben.
 */
@Getter
public class FieldDefinition {

    private final int position;
    private final FieldType type;
    private final boolean isMandatory;
    private final int maxLength;
    private final String name;
    private final InputOption inputType;
    private final boolean internal;

    public FieldDefinition(int position, FieldType type, boolean isMandatory, int maxLength, String name,
            InputOption inputType) {
        this(position, type, isMandatory, maxLength, name, inputType, false);
    }

    public FieldDefinition(int position, FieldType type, boolean isMandatory, int maxLength, String name,
            InputOption inputType, boolean internal) {
        this.position = position;
        this.type = type;
        this.isMandatory = isMandatory;
        this.maxLength = maxLength;
        this.name = name;
        this.inputType = inputType;
        this.internal = internal;
    }
}
