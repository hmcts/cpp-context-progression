package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static java.lang.String.format;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class HearingStub {

    public static final String HEARING_COMMAND = "/hearing-service/command/api/rest/hearing/hearings";
    public static final String HEARING_RESPONSE_TYPE = "application/vnd.hearing.initiate+json";
    public static final String HEARING_APPLICATION_TIMELINE = "/hearing-service/query/api/rest/hearing/timeline/%s";
    public static final String HEARING_APPLICATION_TIMELINE_RESPONSE_TYPE = "application/vnd.hearing.application.timeline+json";

    public static void stubInitiateHearing() {
        stubFor(post(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", HEARING_RESPONSE_TYPE)));

        stubFor(get(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));
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

    public static void stubApplicationTimelineWithNoHearings(final String applicationId) {
        stubFor(get(urlPathEqualTo(format(HEARING_APPLICATION_TIMELINE, applicationId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, HEARING_APPLICATION_TIMELINE_RESPONSE_TYPE)
                        .withBody("{\"hearingSummaries\":[]}")));
    }

    public static void verifyNoInitiateCourtHearingForApplication(final String applicationId) {
        final boolean initiated = getListCourtHearingRequestsAsStream()
                .anyMatch(payload -> isInitiateForApplication(payload, applicationId));
        assertFalse(initiated, format("hearing.initiate was sent for application %s before listing confirmed its hearing", applicationId));
    }

    public static void verifyInitiateCourtHearingSentOnce(final String hearingId) {
        verifyPostInitiateCourtHearing(hearingId);
        await().pollInterval(500, MILLISECONDS).during(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(7))
                .until(() -> countInitiateCourtHearingRequests(hearingId) == 1);
    }

    private static long countInitiateCourtHearingRequests(final String hearingId) {
        return getListCourtHearingRequestsAsStream()
                .filter(payload -> {
                    try {
                        return payload.getJSONObject("hearing").get("id").toString().equalsIgnoreCase(hearingId);
                    } catch (JSONException e) {
                        return false;
                    }
                })
                .count();
    }

    private static boolean isInitiateForApplication(final JSONObject payload, final String applicationId) {
        try {
            return payload.getJSONObject("hearing").getJSONArray("courtApplications").getJSONObject(0)
                    .getString("id").equals(applicationId);
        } catch (JSONException e) {
            return false;
        }
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
