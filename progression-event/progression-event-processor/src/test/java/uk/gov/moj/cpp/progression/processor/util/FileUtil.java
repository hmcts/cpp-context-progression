package uk.gov.moj.cpp.progression.processor.util;

import static java.nio.charset.Charset.defaultCharset;
import static javax.json.Json.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {
    }

    public static String getPayload(final String path) {
        String request = null;
        try (final InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream, notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject jsonFromString(String jsonObjectStr) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }


    public static JsonObject givenPayload(final String filePath) {
        try (final InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            assertThat(inputStream, notNullValue());
            final JsonReader jsonReader = createReader(inputStream);
            return jsonReader.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
