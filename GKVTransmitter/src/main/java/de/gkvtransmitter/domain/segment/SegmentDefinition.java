package de.gkvtransmitter.domain.segment;

import java.util.List;

import de.gkvtransmitter.domain.segment.field.FieldDefinition;
import lombok.Getter;

/**
 * Diese Klasse soll spaeter festlegen, welche Felder, Reihenfolgen und
 * Wiederholungen fuer einen Segmenttyp erlaubt sind.
 */
@Getter
public class SegmentDefinition {
    private final List<FieldDefinition> fieldDefinitions;
    private final String name;
    private final boolean repeatable;

    public SegmentDefinition(List<FieldDefinition> fieldDefinitions, String name, boolean repeatable) {
        this.fieldDefinitions = fieldDefinitions;
        this.name = name;
        this.repeatable = repeatable;
    }
}