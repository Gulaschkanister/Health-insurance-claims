package de.gkvtransmitter.model.segment;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.model.segment.field.PersonRole;
import lombok.Getter;

/**
 * Ein einzelnes Feld einer Nachrichtenvorlage, so wie die Oberflaeche es
 * braucht: mit Wert, Typ und allem, was zum Anzeigen und Pruefen noetig ist.
 *
 * <p>{@code maxLength} und {@code beschreibung} kamen am 05.09.2026 dazu. Beide
 * standen in den Segmentdefinitionen schon immer, wurden aber beim Einlesen
 * verworfen. Die Folge war ein Blaupausenformular, das zu jedem Feld nur den
 * Namen zeigte - eine Beschriftung wie "Tarifkennzeichen" ohne jeden Hinweis,
 * was dort hineingehoert oder wie lang es sein darf, obwohl beides in der
 * JSON-Datei danebenstand.</p>
 */
@Getter
public class ValueFieldEntry {

    private final Object value;
    private final String fieldJavaType;
    private final InputOption inputField;
    private final boolean internal;
    private final PersonRole personRole;
    /** Hoechstlaenge laut Segmentdefinition, oder 0 wenn keine angegeben ist. */
    private final int maxLength;
    /** Was in das Feld gehoert, aus der Segmentdefinition. Kann leer sein. */
    private final String beschreibung;

    public ValueFieldEntry(Object value, String fieldJavaType, InputOption inputField, boolean internal) {
        this(value, fieldJavaType, inputField, internal, null, 0, "");
    }

    public ValueFieldEntry(Object value, String fieldJavaType, InputOption inputField, boolean internal,
            PersonRole personRole) {
        this(value, fieldJavaType, inputField, internal, personRole, 0, "");
    }

    public ValueFieldEntry(Object value, String fieldJavaType, InputOption inputField, boolean internal,
            PersonRole personRole, int maxLength, String beschreibung) {
        this.value = value;
        this.fieldJavaType = fieldJavaType == null || fieldJavaType.isBlank() ? "String" : fieldJavaType;
        this.inputField = inputField;
        this.internal = internal;
        this.personRole = personRole;
        this.maxLength = maxLength;
        this.beschreibung = beschreibung == null ? "" : beschreibung;
    }
}
