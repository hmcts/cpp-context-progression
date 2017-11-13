package uk.gov.moj.cpp.progression.helper;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

public class RestHelper {

    public static RequestSpecification reqSpec;
    private static Properties prop;
    private static String baseUri;
    private static String HOST = "localhost";
    private static int PORT = 8080;

    static {
        prop = new Properties();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final InputStream stream = loader.getResourceAsStream("endpoint.properties");
        try {
            prop.load(stream);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        final String configuredHost = System.getProperty("INTEGRATION_HOST_KEY");
        if (StringUtils.isNotBlank(configuredHost)) {
            HOST = configuredHost;
        }
        baseUri = (StringUtils.isNotEmpty(HOST) ? "http://" + HOST + ":" + PORT : prop.getProperty("base-uri"));

        reqSpec = new RequestSpecBuilder().setBaseUri(baseUri).build();
    }

    public static String pollForResponse(final String path, final String mediaType) {
        return poll(requestParams(getQueryUri(path), mediaType)
                .withHeader("CJSCPPUID", UUID.randomUUID().toString()).build())
                .timeout(10, TimeUnit.SECONDS).until(status().is(OK))
                .getPayload();
    }

    public static String getQueryUri(final String path) {
        return baseUri + prop.getProperty("base-uri-query") + path;
    }

    public static JsonObject getJsonObject(final String jsonAsString) {
        JsonObject payload;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    public static Response postCommand(final String uri, final String mediaType,
                                       final String jsonStringBody) throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).body(jsonStringBody)
                .header("CJSCPPUID", UUID.randomUUID().toString()).when().post(uri).then()
                .extract().response();
    }

    public static Response getCommand(final String uri, final String mediaType) throws IOException {
        return given().spec(reqSpec).and().accept(mediaType).header("CJSCPPUID", UUID.randomUUID().toString()).when().get(uri).then()
                .extract().response();
    }

    public static String getCommandUri(final String path) {
        return baseUri + prop.getProperty("base-uri-command") + path;
    }


    public static void createMockEndpoints() {
        StubUtil.resetStubs();
        StubUtil.setupUsersGroupQueryStub();
        AuthorisationServiceStub.stubEnableAllCapabilities();
    }


    public static void assertThatResponseIndicatesFeatureDisabled(Response response) {
        assertResponseStatusCode(HttpStatus.SC_FORBIDDEN, response);
    }

    public static void assertThatRequestIsAccepted(Response response) {
        assertResponseStatusCode(HttpStatus.SC_ACCEPTED, response);
    }

    public static void assertThatResponseIndicatesSuccess(Response response) {
        assertResponseStatusCode(HttpStatus.SC_OK, response);
    }

    public static void assertResponseStatusCode(int statusCode, Response response) {
        assertThat(response.getStatusCode(), equalTo(statusCode));
    }
}
