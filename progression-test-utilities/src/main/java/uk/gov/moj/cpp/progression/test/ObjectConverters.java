package uk.gov.moj.cpp.progression.test;

import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.StringWriter;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectConverters {

    private ObjectConverters() {

    }

    public static <T> T convert(Class<T> clazz, JsonObject source) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            final T object = mapper.readValue(mapper.writeValueAsString(source), clazz);
            if (object == null) {
                throw new ConverterException(String.format("Failed to convert %s to Object", source));
            } else {
                return object;
            }
        } catch (IOException var4) {
            throw new IllegalArgumentException(String.format("Error while converting %s to JsonObject", source), var4);
        }
    }

    public static String toJsonString(final Object obj) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            final StringWriter writer = new StringWriter();
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, obj);
            return writer.toString();
        } catch (IOException var4) {
            throw new IllegalArgumentException(String.format("Error while writing as json %s to JsonObject", obj), var4);
        }
    }


    public static <T> T asPojo(JsonEnvelope jsonEnvelope, Class<T> clazz) {
        return convert(clazz, jsonEnvelope.payloadAsJsonObject());
    }
}
