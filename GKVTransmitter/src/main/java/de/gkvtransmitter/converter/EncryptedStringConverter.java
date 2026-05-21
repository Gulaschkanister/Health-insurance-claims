package de.gkvtransmitter.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import de.gkvtransmitter.util.security.DataEncryption;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return DataEncryption.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return DataEncryption.decrypt(dbData);
    }
}
