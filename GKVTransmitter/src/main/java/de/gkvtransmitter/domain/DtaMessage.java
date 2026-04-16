package de.gkvtransmitter.domain;

import java.util.List;
import java.util.Set;

import de.gkvtransmitter.definition.InvoiceType;
import lombok.Getter;

/**
 * Diese Klasse speichert eine komplette DTA-Nachricht mit Metadaten,
 * Nachrichtentypen (SLGA/SLLA - mehrere möglich) und ihrer Segmentreihenfolge.
 */
@Getter
public class DtaMessage {

	private final String sourceName;
	private final String invoicerName;
	private final String schemaVersion;
	private final String version;
	private final Set<InvoiceType> messageTypes;
	private final List<SegmentInfo> segments;

	public DtaMessage(String sourceName, String invoicerName, String schemaVersion, String version,
			Set<InvoiceType> messageTypes, List<SegmentInfo> segments) {
		this.sourceName = sourceName;
		this.invoicerName = invoicerName;
		this.schemaVersion = schemaVersion;
		this.version = version;
		this.messageTypes = Set.copyOf(messageTypes);
		this.segments = List.copyOf(segments);
	}
}