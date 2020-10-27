package cz.muni.ics.oidc.models.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.nimbusds.jose.JWSAlgorithm;

@Converter
public class JWSAlgorithmStringConverter implements AttributeConverter<JWSAlgorithm, String> {

    @Override
    public String convertToDatabaseColumn(JWSAlgorithm attribute) {
        return attribute != null ? attribute.getName() : null;
    }

    @Override
    public JWSAlgorithm convertToEntityAttribute(String dbData) {
        return dbData != null ? JWSAlgorithm.parse(dbData) : null;
    }

}
