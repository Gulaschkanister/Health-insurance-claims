package de.gkvtransmitter.domain;

import java.util.List;

import de.gkvtransmitter.definition.InvoiceType;
import de.gkvtransmitter.domain.segment.Segment;
import lombok.Getter;

@Getter
public class Invoice {
    private final List<Segment> segments;
    private final InvoiceType messageType;

    public Invoice(List<Segment> segments, InvoiceType messageType) {
        this.segments = segments;
        this.messageType = messageType;
    }
}