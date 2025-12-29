package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.json.JsonObject;
import javax.json.JsonReader;

public class JsonHelper {
    public static JsonObject readJson(final String filePath) {
        try (final InputStream inputStream = JsonHelper.class.getResourceAsStream(filePath);
             final JsonReader jsonReader = createReader(inputStream)) {
            return jsonReader.readObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonObject readJsonViaPath(final String filePath) throws IOException {

        final byte[] buf = readAllBytes(get(filePath));
        try (final InputStream inputStream = new ByteArrayInputStream(buf);
             final JsonReader jsonReader = createReader(inputStream)) {
            return jsonReader.readObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}