package cz.muni.ics.oidc.models.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Converter
public class SimpleGrantedAuthorityStringConverter implements AttributeConverter<SimpleGrantedAuthority, String> {

    @Override
    public String convertToDatabaseColumn(SimpleGrantedAuthority attribute) {
        return attribute != null ? attribute.getAuthority() : null;
    }

    @Override
    public SimpleGrantedAuthority convertToEntityAttribute(String dbData) {
        return dbData != null ? new SimpleGrantedAuthority(dbData) : null;
    }

}

