package de.gkvtransmitter.domain;

import java.util.List;

import lombok.Getter;

/**
 * Diese Klasse soll spaeter festlegen, welche Felder, Reihenfolgen und
 * Wiederholungen fuer einen Segmenttyp erlaubt sind.
 */
@Getter
public class SegmentDefinition {
    private List<FieldDefinition> fieldDefinitions;
    private String name;
    private boolean repeatable;

    public SegmentDefinition(List<FieldDefinition> fieldDefinitions, String name, boolean repeatable) {
        this.fieldDefinitions = fieldDefinitions;
        this.name = name;
        this.repeatable = repeatable;
    }
}