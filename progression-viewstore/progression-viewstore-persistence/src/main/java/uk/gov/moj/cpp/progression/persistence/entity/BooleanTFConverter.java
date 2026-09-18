package uk.gov.moj.cpp.progression.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Converter
public class BooleanTFConverter implements AttributeConverter<Boolean, String> {
    @Override
    public String convertToDatabaseColumn(final Boolean value) {
        if (value == null) {
            return null;
        }
        if (Boolean.TRUE.equals(value)) {
            return "T";
        } else {
            return "F";
        }
    }
    @Override
    public Boolean convertToEntityAttribute(final String value) {
        Boolean returnValue = null;
        if (value != null) {
            returnValue = "T".equals(value);
        }
        return returnValue;
    }
}