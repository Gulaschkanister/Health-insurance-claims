package de.gkvtransmitter.domain;

import lombok.Getter;

@Getter
public class ValueFieldEntry {

    private final Object value;
    private final String fieldJavaType;

    public ValueFieldEntry(Object value, String fieldJavaType) {
        this.value = value;
        this.fieldJavaType = fieldJavaType == null || fieldJavaType.isBlank() ? "String" : fieldJavaType;
    }
}
