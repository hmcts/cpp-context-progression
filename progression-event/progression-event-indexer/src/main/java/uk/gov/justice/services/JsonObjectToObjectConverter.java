package uk.gov.justice.services;

import uk.gov.justice.services.common.converter.JSONObjectValueObfuscator;
import uk.gov.justice.services.common.converter.TypedConverter;
import uk.gov.justice.services.common.converter.exception.ConverterException;

import java.io.IOException;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonObjectToObjectConverter implements TypedConverter<JsonObject, Object> {
    private ObjectMapper mapper;

    public JsonObjectToObjectConverter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @SuppressWarnings("squid:S1166")//Severity: CRITICAL, Message: Either log or rethrow this exception
    public <R> R convert(final JsonObject source, final Class<R> clazz) {
        try {
            final R object = this.mapper.readValue(this.mapper.writeValueAsString(source), clazz);
            if (object == null) {
                throw new ConverterException(String.format("Error while converting to %s from json:[%s]", clazz.getName(), JSONObjectValueObfuscator.obfuscated(source)));
            } else {
                return object;
            }
        } catch (final IOException var4) {
            throw new IllegalArgumentException(String.format("Error while converting to %s from json (obfuscated):[%s]", clazz.getName(), JSONObjectValueObfuscator.obfuscated(source)));
        }
    }
}