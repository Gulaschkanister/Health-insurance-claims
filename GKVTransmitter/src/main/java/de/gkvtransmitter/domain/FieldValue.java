package de.gkvtransmitter.domain;

import java.util.List;

import lombok.Getter;

/**
 * Diese Klasse soll spaeter den konkreten Inhalt eines einzelnen Feldes
 * innerhalb eines Segments halten.
 */
@Getter
public class FieldValue {
    private FieldDefinition definition;
    private Object value;

    public FieldValue(FieldDefinition definition, Object value) {
        this.definition = definition;
        this.value = value;
    }
}