package cz.muni.ics.oidc.models.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.nimbusds.jose.EncryptionMethod;

@Converter
public class JWEEncryptionMethodStringConverter implements AttributeConverter<EncryptionMethod, String> {

    @Override
    public String convertToDatabaseColumn(EncryptionMethod attribute) {
        return attribute != null ? attribute.getName() : null;
    }

    @Override
    public EncryptionMethod convertToEntityAttribute(String dbData) {
        return dbData != null ? EncryptionMethod.parse(dbData) : null;
    }

}
