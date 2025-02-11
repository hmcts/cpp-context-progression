package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.io.StringReader;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;

public class IngesterUtil {
    private static final Poller poller = new Poller(1000, 10L);
    
    public static JsonObject jsonFromString(final String jsonObjectStr) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    public static String getStringFromResource(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static Poller getPoller() {
        return poller;
    }
}
