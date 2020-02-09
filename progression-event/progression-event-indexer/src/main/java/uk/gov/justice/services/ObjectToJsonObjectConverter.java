package uk.gov.justice.services;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.exception.ConverterException;

import java.io.IOException;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectToJsonObjectConverter implements Converter<Object, JsonObject> {

    private ObjectMapper mapper;

    public ObjectToJsonObjectConverter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @SuppressWarnings("squid:S1166")//Severity: CRITICAL, Message: Either log or rethrow this exception
    public JsonObject convert(final Object source) {
        try {
            final JsonObject jsonObject = this.mapper.readValue(this.mapper.writeValueAsString(source), JsonObject.class);
            if (jsonObject == null) {
                throw new ConverterException(String.format("Failed to convert %s to JsonObject", source));
            } else {
                return jsonObject;
            }
        } catch (final IOException var3) {
            final String errorMessage = String.format("Error while converting %s toJsonObject", source);
            throw new IllegalArgumentException(errorMessage, var3);
        }
    }
}