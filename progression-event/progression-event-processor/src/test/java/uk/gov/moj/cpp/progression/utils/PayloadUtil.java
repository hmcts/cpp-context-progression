package uk.gov.moj.cpp.progression.utils;

import static java.nio.charset.Charset.defaultCharset;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.File;
import java.io.IOException;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

public class PayloadUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private PayloadUtil() {
        // default private constructor
    }

    public static JsonObject getPayloadAsJsonObject(final String filename) throws IOException {
        String response = Resources.toString(Resources.getResource(filename), defaultCharset());
        return new StringToJsonObjectConverter().convert(response);
    }

    public static <T> T convertFromFile(final String url, final Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(new File(PayloadUtil.class.getClassLoader().getResource(url).getFile()), clazz);
    }
}
