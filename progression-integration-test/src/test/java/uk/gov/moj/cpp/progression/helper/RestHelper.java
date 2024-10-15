package uk.gov.moj.cpp.progression.helper;

import static io.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.jayway.jsonpath.ReadContext;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;

public class RestHelper {

    public static final int TIMEOUT = 90;
    public static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int PORT = 8080;
    private static final String BASE_URI = "http://" + HOST + ":" + PORT;

    private static final RestClient restClient = new RestClient();
    private static final RequestSpecification REQUEST_SPECIFICATION = new RequestSpecBuilder().setBaseUri(BASE_URI).build();
    private static final int POLL_INTERVAL = 2;

    public static RequestSpecification getRequestSpecification() {
        return REQUEST_SPECIFICATION;
    }

    public static javax.ws.rs.core.Response getMaterialContentResponse(final String path, final UUID userId, final String mediaType) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        map.add(HttpHeaders.ACCEPT, mediaType);
        return restClient.query(getReadUrl(path), mediaType, map);
    }

    public static String pollForResponse(final String path, final String mediaType) {
        return pollForResponse(path, mediaType, randomUUID().toString(), status().is(OK));
    }

    public static String pollForResponse(final String path, final String mediaType, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, randomUUID().toString(), payloadMatchers);
    }

    public static String pollForResponse(final String path, final String mediaType, final int timeOutInSeconds, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, randomUUID().toString(), timeOutInSeconds, payloadMatchers);
    }

    public static String pollForResponse(final String path, final String mediaType, final String userId, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, userId, status().is(OK), payloadMatchers);
    }

    public static String pollForResponse(final String path, final String mediaType, final String userId, final int timeOutInSeconds, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, userId, status().is(OK), timeOutInSeconds, payloadMatchers);
    }


    public static String pollForResponse(final String path, final String mediaType, final String userId, final ResponseStatusMatcher responseStatusMatcher, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, userId, responseStatusMatcher, TIMEOUT, payloadMatchers);
    }


    public static String pollForResponse(final String path, final String mediaType, final String userId, final ResponseStatusMatcher responseStatusMatcher, final int timeOutInSeconds, final Matcher... payloadMatchers) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(timeOutInSeconds, TimeUnit.SECONDS)
                .until(
                        responseStatusMatcher,
                        payload().isJson(allOf(payloadMatchers))
                )
                .getPayload();
    }

    public static String pollForResponse(final String path,
                                         final String mediaType,
                                         final String userId, List<Matcher<? super ReadContext>> matchers) {
        return poll(requestParams(getReadUrl(path),
                mediaType)
                .withHeader(USER_ID, userId))
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))).getPayload();

    }

    public static JsonObject getJsonObject(final String jsonAsString) {
        final JsonObject payload;
        try (final JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    public static Response postCommand(final String uri, final String mediaType,
                                       final String jsonStringBody) throws IOException {
        return postCommandWithUserId(uri, mediaType, jsonStringBody, randomUUID().toString());
    }

    public static Response postCommandWithUserId(final String uri, final String mediaType,
                                                  final String jsonStringBody, final String userId) throws IOException {
        return given().spec(REQUEST_SPECIFICATION).and().contentType(mediaType).body(jsonStringBody)
                .header(USER_ID, userId).when().post(uri).then()
                .extract().response();
    }

    public static void assertThatRequestIsAccepted(final Response response) {
        assertResponseStatusCode(HttpStatus.SC_ACCEPTED, response);
    }

    private static void assertResponseStatusCode(final int statusCode, final Response response) {
        assertThat(response.getStatusCode(), equalTo(statusCode));
    }
}
