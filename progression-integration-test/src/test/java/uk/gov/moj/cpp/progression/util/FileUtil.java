package uk.gov.moj.cpp.progression.util;

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.io.InputStream;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(final String path) {
        String fileContents = null;
        try (final InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream, notNullValue());
            fileContents = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return fileContents;
    }

    public static JsonObject getPayloadAsJsonObject(final String path) {
        return new StringToJsonObjectConverter().convert(getPayload(path));
    }

}
