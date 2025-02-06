package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.waitAtMost;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class HearingStub {

    public static final String HEARING_COMMAND = "/hearing-service/command/api/rest/hearing/hearings";
    public static final String HEARING_RESPONSE_TYPE = "application/vnd.hearing.initiate+json";

    public static void stubInitiateHearing() {
        stubFor(post(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", HEARING_RESPONSE_TYPE)));

        stubFor(get(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    public static void verifyPostInitiateCourtHearing(final String hearingId) {
        waitAtMost(Duration.ofSeconds(10)).until(() -> {
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
