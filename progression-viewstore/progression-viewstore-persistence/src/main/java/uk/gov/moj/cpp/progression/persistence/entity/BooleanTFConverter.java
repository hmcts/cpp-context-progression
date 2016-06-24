package uk.gov.moj.cpp.progression.persistence.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class BooleanTFConverter implements AttributeConverter<Boolean, String> {
    @Override
    public String convertToDatabaseColumn(Boolean value) {
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
    public Boolean convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return "T".equals(value);
    }
}