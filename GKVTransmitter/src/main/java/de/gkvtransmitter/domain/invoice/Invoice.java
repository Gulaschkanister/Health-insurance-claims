package de.gkvtransmitter.domain.invoice;

import java.util.Map;

import de.gkvtransmitter.definition.InvoiceType;
import de.gkvtransmitter.domain.segment.SegmentDefinition;
import lombok.Getter;

@Getter
public class Invoice {
    private final Map<String, SegmentDefinition> segments;
    private final InvoiceType messageType;

    public Invoice(Map<String, SegmentDefinition> segments, InvoiceType messageType) {
        this.segments = Map.copyOf(segments);
        this.messageType = messageType;
    }
}