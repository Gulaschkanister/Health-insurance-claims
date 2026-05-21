package de.gkvtransmitter.converter;

import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import de.gkvtransmitter.util.security.DataEncryption;

@Converter
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) {
            return null;
        }
        return DataEncryption.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        String decrypted = DataEncryption.decrypt(dbData);
        if (decrypted == null || decrypted.isBlank()) {
            return null;
        }
        return LocalDate.parse(decrypted);
    }
}
