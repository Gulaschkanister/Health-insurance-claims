package de.gkvtransmitter.domain;

import de.gkvtransmitter.definition.InvoiceType;
import lombok.Getter;

/**
 * Speichert Informationen über ein einzelnes Segment in einer DTA-Nachricht.
 * Kombiniert Position, Segment-Typ und den zugehörigen Nachrichtentyp (SLGA/SLLA).
 */
@Getter
public class SegmentInfo {
    private final int position;
    private final String segmentType;
    private final InvoiceType messageType;  // nullable für Header/Footer wie UNB/UNZ

    public SegmentInfo(int position, String segmentType, InvoiceType messageType) {
        this.position = position;
        this.segmentType = segmentType;
        this.messageType = messageType;
    }
}
