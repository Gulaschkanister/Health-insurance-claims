package de.gkvtransmitter.model.segment;

import java.util.List;

import de.gkvtransmitter.model.segment.field.FieldValue;
import lombok.Getter;

/**
 * Diese Klasse soll spaeter ein einzelnes fachliches DTA-Segment mit seinen
 * konkreten Feldwerten repraesentieren.
 */
@Getter
public class Segment {

    private final List<FieldValue> fieldValues;
    private final SegmentDefinition definition;

    public Segment(List<FieldValue> fieldValues, SegmentDefinition definition) {
        this.fieldValues = fieldValues;
        this.definition = definition;
    }

}
