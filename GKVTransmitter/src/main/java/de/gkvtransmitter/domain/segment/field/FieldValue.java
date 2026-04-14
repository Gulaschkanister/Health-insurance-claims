package de.gkvtransmitter.domain.segment.field;

import lombok.Getter;

/**
 * Diese Klasse soll spaeter den konkreten Inhalt eines einzelnen Feldes
 * innerhalb eines Segments halten.
 */
@Getter
public class FieldValue {
    private final FieldDefinition definition;
    private final Object value;

    public FieldValue(FieldDefinition definition, Object value) {
        this.definition = definition;
        this.value = value;
    }
}