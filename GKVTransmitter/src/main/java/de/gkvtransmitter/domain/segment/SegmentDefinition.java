package de.gkvtransmitter.domain.segment;

import java.util.Map;

import de.gkvtransmitter.domain.segment.field.FieldDefinition;
import lombok.Getter;

/**
 * Diese Klasse soll spaeter festlegen, welche Felder, Reihenfolgen und
 * Wiederholungen fuer einen Segmenttyp erlaubt sind.
 */
@Getter
public class SegmentDefinition {
    private final Map<Integer, FieldDefinition> fieldDefinitions;
    private final String name;
    private final boolean repeatable;

    public SegmentDefinition(Map<Integer, FieldDefinition> fieldDefinitions, String name, boolean repeatable) {
        this.fieldDefinitions = Map.copyOf(fieldDefinitions);
        this.name = name;
        this.repeatable = repeatable;
    }
}