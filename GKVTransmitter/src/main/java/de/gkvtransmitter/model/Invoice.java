package de.gkvtransmitter.model;

import java.util.Map;

import de.gkvtransmitter.definition.InvoiceType;
import de.gkvtransmitter.model.segment.SegmentDefinition;
import lombok.Getter;

@Getter
public class Invoice implements DtaProfile {

    private final Map<String, SegmentDefinition> segments;
    private final InvoiceType messageType;

    public Invoice(Map<String, SegmentDefinition> segments, InvoiceType messageType) {
        this.segments = Map.copyOf(segments);
        this.messageType = messageType;
    }
}
