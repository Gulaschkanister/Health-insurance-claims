package de.gkvtransmitter.domain;

import de.gkvtransmitter.domain.inputOptions.InputOptions;
import lombok.Getter;

@Getter
public class ValueFieldEntry {

    private final Object value;
    private final String fieldJavaType;
    private final InputOptions inputField;

    public ValueFieldEntry(Object value, String fieldJavaType, InputOptions inputField) {
        this.value = value;
        this.fieldJavaType = fieldJavaType == null || fieldJavaType.isBlank() ? "String" : fieldJavaType;
        this.inputField = inputField;
    }
}
