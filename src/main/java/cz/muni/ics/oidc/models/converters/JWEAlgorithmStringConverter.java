package cz.muni.ics.oidc.models.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.nimbusds.jose.JWEAlgorithm;

@Converter
public class JWEAlgorithmStringConverter implements AttributeConverter<JWEAlgorithm, String> {

    @Override
    public String convertToDatabaseColumn(JWEAlgorithm attribute) {
        return attribute != null ? attribute.getName() : null;
    }

    @Override
    public JWEAlgorithm convertToEntityAttribute(String dbData) {
        return dbData != null ? JWEAlgorithm.parse(dbData) : null;
    }

}
