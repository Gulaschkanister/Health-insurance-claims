package de.gkvtransmitter.domain;

import java.util.List;

import lombok.Getter;

/**
 * Diese Klasse soll spaeter eine komplette DTA-Nachricht mit Metadaten und
 * ihrer Segmentreihenfolge beschreiben.
 */
@Getter
public class DtaMessage {
	private final String sourceName;
	private final String invoicerName;
	private final String schemaVersion;
	private final String version;
	private final List<String> segmentTypes;

	public DtaMessage(String sourceName, String invoicerName, String schemaVersion, String version,
			List<String> segmentTypes) {
		this.sourceName = sourceName;
		this.invoicerName = invoicerName;
		this.schemaVersion = schemaVersion;
		this.version = version;
		this.segmentTypes = List.copyOf(segmentTypes);
	}
}