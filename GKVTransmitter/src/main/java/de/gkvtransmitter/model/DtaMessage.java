package de.gkvtransmitter.model;

import java.util.List;

import de.gkvtransmitter.model.segment.SegmentInfo;
import lombok.Getter;

/**
 * Diese Klasse speichert eine komplette DTA-Nachricht mit Metadaten,
 * Segmentreihenfolge und Segmentdetails.
 */
@Getter
public class DtaMessage {

    private final String sourceName;
    private final String invoicerName;
    private final String schemaVersion;
    private final String version;
    private final List<SegmentInfo> segments;

    public DtaMessage(String sourceName, String invoicerName, String schemaVersion, String version,
            List<SegmentInfo> segments) {
        this.sourceName = sourceName;
        this.invoicerName = invoicerName;
        this.schemaVersion = schemaVersion;
        this.version = version;
        this.segments = List.copyOf(segments);
    }
}
