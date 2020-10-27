package cz.muni.ics.oidc.models.converters;

import java.text.ParseException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

@Converter
public class JWTStringConverter implements AttributeConverter<JWT, String> {

    @Override
    public String convertToDatabaseColumn(JWT attribute) {
        return attribute != null ? attribute.serialize() : null;
    }

    @Override
    public JWT convertToEntityAttribute(String dbData) {
        if (dbData != null) {
            try {
                return JWTParser.parse(dbData);
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

}

