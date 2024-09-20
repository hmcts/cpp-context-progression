package uk.gov.justice.api.resource.utils;

import static java.nio.charset.Charset.defaultCharset;
import static javax.json.Json.createReader;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {}

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final IOException e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject jsonFromString(final String jsonObjectStr) {

        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    public static JsonObject jsonFromPath(final String path) {
        return jsonFromString(getPayload(path));
    }

    public static JsonObject givenPayload(final String filePath) throws IOException {
        JsonReader jsonReader = null;
        try (final InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            jsonReader = createReader(inputStream);
            return jsonReader.readObject();
        } finally {
            if (Objects.nonNull(jsonReader)) {
                jsonReader.close();
            }
        }
    }
}
