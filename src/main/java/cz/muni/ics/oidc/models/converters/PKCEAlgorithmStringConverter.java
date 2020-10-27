package cz.muni.ics.oidc.models.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import cz.muni.ics.oidc.models.PKCEAlgorithm;

@Converter
public class PKCEAlgorithmStringConverter implements AttributeConverter<PKCEAlgorithm, String> {

    @Override
    public String convertToDatabaseColumn(PKCEAlgorithm attribute) {
        return attribute != null ? attribute.getName() : null;
    }

    @Override
    public PKCEAlgorithm convertToEntityAttribute(String dbData) {
        return dbData != null ? PKCEAlgorithm.parse(dbData) : null;
    }

}

