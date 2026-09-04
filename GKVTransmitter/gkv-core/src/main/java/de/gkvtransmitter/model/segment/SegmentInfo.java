package de.gkvtransmitter.model.segment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.gkvtransmitter.definition.InvoiceType;
import lombok.Getter;

/**
 * Speichert Informationen über ein einzelnes Segment in einer DTA-Nachricht.
 * Kombiniert Position, Segment-Typ und den zugehörigen Nachrichtentyp
 * (SLGA/SLLA).
 */
@Getter
public class SegmentInfo {

    private final int position;
    private final String segmentType;
    private final InvoiceType messageType;  // nullable für Header/Footer wie UNB/UNZ
    private final String groupTag;
    private final Map<String, ValueFieldEntry> valueFields;

    public SegmentInfo(int position, String segmentType, InvoiceType messageType, String groupTag,
            Map<String, ValueFieldEntry> valueFields) {
        this.position = position;
        this.segmentType = segmentType;
        this.messageType = messageType;
        this.groupTag = groupTag == null ? "" : groupTag;
        this.valueFields = valueFields == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(valueFields));
    }
}
