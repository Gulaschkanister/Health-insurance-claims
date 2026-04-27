package de.gkvtransmitter.model.segment;

import de.gkvtransmitter.enums.InputOption;
import lombok.Getter;

@Getter
public class ValueFieldEntry {

    private final Object value;
    private final String fieldJavaType;
    private final InputOption inputField;
    private final boolean internal;

    public ValueFieldEntry(Object value, String fieldJavaType, InputOption inputField, boolean internal) {
        this.value = value;
        this.fieldJavaType = fieldJavaType == null || fieldJavaType.isBlank() ? "String" : fieldJavaType;
        this.inputField = inputField;
        this.internal = internal;
    }
}
