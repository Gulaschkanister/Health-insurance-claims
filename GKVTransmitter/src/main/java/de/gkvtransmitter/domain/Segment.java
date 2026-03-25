package de.gkvtransmitter.domain;

import java.util.List;

import lombok.Getter;

/**
 * Diese Klasse soll spaeter ein einzelnes fachliches DTA-Segment mit seinen
 * konkreten Feldwerten repraesentieren.
 */
@Getter
public class Segment {
    private List<FieldValue> fieldValues;
    private SegmentDefinition definition;

    public Segment(List<FieldValue> fieldValues, SegmentDefinition definition) {
        this.fieldValues = fieldValues;
        this.definition = definition;
    }

}