package de.gkvtransmitter.domain;

import lombok.Getter;

/**
 * Diese Klasse soll spaeter die Regeln fuer ein einzelnes Feld wie Typ,
 * Pflichtstatus und Laengenbegrenzung beschreiben.
 */
@Getter
public class FieldDefinition {
    private FieldType type;
    private boolean isMandatory;
    private int maxLength;
    private String name;

    public FieldDefinition(FieldType type, boolean isMandatory, int maxLength, String name) {
        this.type = type;
        this.isMandatory = isMandatory;
        this.maxLength = maxLength;
        this.name = name;
    }
}