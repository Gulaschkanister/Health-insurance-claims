package de.gkvtransmitter.model;

import java.util.Map;

import de.gkvtransmitter.definition.InvoiceType;
import de.gkvtransmitter.model.segment.SegmentDefinition;

/**
 * Dieses Interface soll spaeter alle Definitionen und Regeln eines konkreten
 * DTA- oder Abrechnungsprofils zusammenfassen.
 */
public interface DtaProfile {

    /**
     * Liefert alle Segmentdefinitionen des Profils.
     */
    Map<String, SegmentDefinition> getSegments();

    /**
     * Liefert den Nachrichtentyp (SLLA/SLGA).
     */
    InvoiceType getMessageType();

    /**
     * Prüft, ob eine Segmentdefinition mit dem angegebenen Namen vorhanden ist.
     */
    default boolean hasSegment(String segmentName) {
        return segmentName != null && getSegments().containsKey(segmentName);
    }
}
