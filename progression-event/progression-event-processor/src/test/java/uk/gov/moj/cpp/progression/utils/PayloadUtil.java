package uk.gov.moj.cpp.progression.utils;

import static java.nio.charset.Charset.defaultCharset;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.io.IOException;

import javax.json.JsonObject;

import com.google.common.io.Resources;

public class PayloadUtil {

    private PayloadUtil() {
        // default private constructor
    }

    public static JsonObject getPayloadAsJsonObject(final String filename) throws IOException {
        String response = Resources.toString(Resources.getResource(filename), defaultCharset());
        return new StringToJsonObjectConverter().convert(response);
    }
}
