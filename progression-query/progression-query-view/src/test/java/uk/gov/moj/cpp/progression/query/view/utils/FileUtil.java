package uk.gov.moj.cpp.progression.query.view.utils;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static java.nio.charset.Charset.defaultCharset;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.junit.jupiter.api.Assertions.fail;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {

    }

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

    public static JsonObject getJsonPayload(final String fileName) throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(fileName);
             final JsonReader jsonReader = JsonObjects.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            return payload;
        }

    }
}