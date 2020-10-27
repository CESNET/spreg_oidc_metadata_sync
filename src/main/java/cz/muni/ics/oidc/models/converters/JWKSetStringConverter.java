package cz.muni.ics.oidc.models.converters;

import java.text.ParseException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.nimbusds.jose.jwk.JWKSet;

@Converter
public class JWKSetStringConverter implements AttributeConverter<JWKSet, String> {

    @Override
    public String convertToDatabaseColumn(JWKSet attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public JWKSet convertToEntityAttribute(String dbData) {
        if (dbData != null) {
            try {
                return JWKSet.parse(dbData);
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

}

