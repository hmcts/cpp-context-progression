package uk.gov.moj.cpp.progression.helper;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockHelper {

    /**
     * Create a stub for the {@link List} of endpoints on the deployed WireMock WAR
     *
     * @param endpoints A {@link List<Endpoint>} of endpoints describing the stubs to be created in
     *        the WireMock server
     */
    public static void stub(List<Endpoint> endpoints) {
        endpoints.stream().forEach(WireMockHelper::stub);
    }

    /**
     * Create a stub for the {@link Endpoint} of endpoints on the deployed WireMock WAR
     *
     * @param endpoint {@link Endpoint} Describing the endpoint stub to be created in the WireMock
     *        server
     */
    public static void stub(Endpoint endpoint) {
        stubFor(endpoint.getRequestType().apply(urlMatching(endpoint.getUrl()))
                        .willReturn(aResponse().withStatus(endpoint.getStatus())
                                        .withHeader("Content-Type", endpoint.getContentType())
                                        .withHeader("CPPID", UUID.randomUUID().toString())
                                        .withBody(endpoint.getBody().toString())));
    }

    /**
     * Method to load json file from file system and build json object from it
     *
     * @param resource The path of the resource on the file system
     * @return {@link JsonObject} representing the json object file specified by resource parameter
     * @throws IOException
     */
    public static JsonObject getPayload(String resource) {
        JsonObject payload;
        ByteSource source = Resources.asByteSource(Resources.getResource(resource));

        try (InputStream inputStream = source.openBufferedStream()) {

            JsonReader jsonReader = Json.createReader(inputStream);
            payload = jsonReader.readObject();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load payload from " + resource);
        }

        return payload;
    }

    /**
     * Method to convert String to json Object
     *
     * @param jsonAsString The string representation of Json
     * @return {@link JsonObject} representing the json object file specified by resource parameter
     * @throws IOException
     */
    public static JsonObject getJsonObject(String jsonAsString) {
        JsonObject payload;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    public static String getPayloadAsString(String resource) {
        String payload;
        ByteSource source = Resources.asByteSource(Resources.getResource(resource));

        try (InputStream inputStream = source.openBufferedStream()) {

            JsonReader jsonReader = Json.createReader(inputStream);
            payload = jsonReader.readObject().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load payload from " + resource);
        }

        return payload;
    }
}
