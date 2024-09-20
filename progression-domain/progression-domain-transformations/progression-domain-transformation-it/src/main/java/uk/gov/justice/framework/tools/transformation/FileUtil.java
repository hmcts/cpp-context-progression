package uk.gov.justice.framework.tools.transformation;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static String getFileContent(final String path, final Map<String, Object> namedPlaceholders) {
        return new StrSubstitutor(namedPlaceholders).replace(getPayload(path));
    }

    public static JsonObject getFileContentAsJson(final String path, final Map<String, Object> namedPlaceholders) {
        return getJsonObject(getFileContent(path, namedPlaceholders));
    }

    public static JsonObject getFileContentAsJson(final String path) {
        return getJsonObject(getFileContent(path, emptyMap()));
    }

    public static JsonObject getJsonObject(final String json) {
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
