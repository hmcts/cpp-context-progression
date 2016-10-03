package uk.gov.moj.cpp.progression.helper;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            LOGGER.error(String.format("Error consuming file from location {}", path), e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject givenPayload(final String filePath) throws IOException {
        try (InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            final JsonReader jsonReader = Json.createReader(inputStream);
            return jsonReader.readObject();
        }
    }
}
