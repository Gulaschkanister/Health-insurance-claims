package de.gkvtransmitter.domain.segment.field;

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

    public FieldDefinition(int position, FieldType type, boolean isMandatory, int maxLength, String name) {
        this.position = position;
        this.type = type;
        this.isMandatory = isMandatory;
        this.maxLength = maxLength;
        this.name = name;
    }
}