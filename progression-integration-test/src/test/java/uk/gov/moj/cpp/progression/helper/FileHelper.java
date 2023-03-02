package uk.gov.moj.cpp.progression.helper;

import static java.lang.String.format;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class FileHelper {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    public static JsonObject readJson(final String path, final Object... placeholders) {
        return stringToJsonObject(read(path, placeholders));
    }

    public static JsonObject readJson(final Path path, final Object... placeholders) {
        return readJson(path.toString(), placeholders);
    }

    public static <T> T readJson(final String path, Class<T> clazz, final Map<String, String> placeholders) {
        final String loadedFile = new StrSubstitutor(placeholders).replace(read(path));
        try {
            return objectMapper.readValue(loadedFile, clazz);
        } catch (IOException ioe) {
            throw new AssertionError(format("Could not deserialize %s into %s", loadedFile, clazz.getSimpleName()));
        }
    }

    public static String read(final String path, final Object... placeholders) {
        try (final InputStream resourceAsStream = FileHelper.class.getClassLoader().getResourceAsStream(path)) {
            if (resourceAsStream == null) {
                throw new AssertionError("File does not exist. " + path);
            }

            final String template = IOUtils.toString(resourceAsStream);
            return placeholders.length > 0 ? String.format(template, placeholders) : template;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject stringToJsonObject(final String content) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(content))) {
            return jsonReader.readObject();
        }
    }

}
