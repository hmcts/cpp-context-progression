package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.waitAtMost;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.time.Duration;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class HearingStub {

    public static final String HEARING_COMMAND = "/hearing-service/command/api/rest/hearing/hearings";
    public static final String HEARING_RESPONSE_TYPE = "application/vnd.hearing.initiate+json";
    public static final String HEARING_QUERY = "/hearing-service/query/api/rest/hearing/hearings/{0}";

    public static void stubInitiateHearing() {
        stubFor(post(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", HEARING_RESPONSE_TYPE)));

        stubFor(get(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    /**
     * Stubs hearing.get.hearing for the given hearing so that it lists the given court applications
     * (none = a cases-only hearing).
     */
    public static void stubGetHearingWithCourtApplications(final String hearingId, final JsonObject... courtApplications) {
        final JsonArrayBuilder courtApplicationsArray = Stream.of(courtApplications)
                .reduce(createArrayBuilder(), JsonArrayBuilder::add, (left, right) -> left);
        final JsonObject hearingResponse = createObjectBuilder()
                .add("hearing", createObjectBuilder()
                        .add("id", hearingId)
                        .add("courtApplications", courtApplicationsArray))
                .build();

        stubFor(get(urlPathEqualTo(format(HEARING_QUERY, hearingId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(hearingResponse.toString())));
    }

    public static void verifyPostInitiateCourtHearing(final String hearingId) {
        waitAtMost(Duration.ofSeconds(10)).pollInterval(500, MILLISECONDS).until(() -> {
                    final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStream();
                    return listCourtHearingRequestsAsStream.anyMatch(
                            payload -> {
                                try {
                                    return payload.getJSONObject("hearing").get("id").toString().equalsIgnoreCase(hearingId);
                                } catch (JSONException e) {
                                    return false;
                                }
                            }
                    );
                }
        );

    }

    private static Stream<JSONObject> getListCourtHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(HEARING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(HEARING_RESPONSE_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(t -> {
                    try {
                        return new JSONObject(t);
                    } catch (JSONException e) {
                        return null;
                    }
                });
    }
}
