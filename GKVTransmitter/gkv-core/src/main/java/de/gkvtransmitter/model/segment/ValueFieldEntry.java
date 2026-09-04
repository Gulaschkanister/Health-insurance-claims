package de.gkvtransmitter.model.segment;

import de.gkvtransmitter.enums.InputOption;
import de.gkvtransmitter.model.segment.field.PersonRole;
import lombok.Getter;

@Getter
public class ValueFieldEntry {

    private final Object value;
    private final String fieldJavaType;
    private final InputOption inputField;
    private final boolean internal;
    private final PersonRole personRole;

    public ValueFieldEntry(Object value, String fieldJavaType, InputOption inputField, boolean internal) {
        this(value, fieldJavaType, inputField, internal, null);
    }

    public ValueFieldEntry(Object value, String fieldJavaType, InputOption inputField, boolean internal,
            PersonRole personRole) {
        this.value = value;
        this.fieldJavaType = fieldJavaType == null || fieldJavaType.isBlank() ? "String" : fieldJavaType;
        this.inputField = inputField;
        this.internal = internal;
        this.personRole = personRole;
    }
}
