package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.json.JSONObject;
import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import javax.json.JsonObject;

public class HearingStub {

    public static final String HEARING_COMMAND = "/hearing-service/command/api/rest/hearing/hearings";
    public static final String HEARING_RESPONSE_TYPE = "application/vnd.hearing.initiate+json";

    private static final String HEARING_SUBSCRIPTION_QUERY_URL =
            "/hearing-service/query/api/rest/hearing/retrieve?referenceDate=%s&nowTypeId=%s";
    private static final String HEARING_SUBSCRIPTION_QUERY_URL2 =
            "/hearing-service/query/api/rest/hearing/retrieve?nowTypeId=%s&referenceDate=%s";

    private static final String HEARING_SUBSCRIPTIONS_MEDIA_TYPE = "application/vnd.hearing.retrieve-subscriptions+json";

    public static void stubInitiateHearing() {
        InternalEndpointMockUtils.stubPingFor("hearing-service");

        stubFor(post(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", HEARING_RESPONSE_TYPE)));

        stubFor(get(urlPathEqualTo(HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(HEARING_COMMAND, HEARING_RESPONSE_TYPE);
    }

    public static void  stubSubscriptions(final JsonObject jsonSubscriptions, UUID nowsTypeId) {

        InternalEndpointMockUtils.stubPingFor("hearing-service");

        final String strSubscriptions = jsonSubscriptions.toString();

        String referenceDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));

        //because the framework changes the parameter order (WTF ?!!?!?!?!?!)
        final String[] urlPaths = {format(HEARING_SUBSCRIPTION_QUERY_URL, referenceDate, nowsTypeId.toString()),
                format(HEARING_SUBSCRIPTION_QUERY_URL2,  nowsTypeId, referenceDate) };
 // TODO https://stackoverflow.com/questions/49549523/how-to-match-request-paramters-in-wire-mock-url
        for (String urlPath : urlPaths) {
            stubFor(get(urlPathEqualTo(urlPath))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader("CPPID", UUID.randomUUID().toString())
                            .withHeader("Content-Type", HEARING_SUBSCRIPTIONS_MEDIA_TYPE)
                            .withBody(strSubscriptions)));

            waitForStubToBeReady(urlPath, HEARING_SUBSCRIPTIONS_MEDIA_TYPE);
            System.out.println("stubbed " + urlPath);
        }

    }

    public static void verifyPostInitiateCourtHearing(final String hearingId) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    getListCourtHearingRequestsAsStream().get()
                            .getJSONObject("hearing").get("id").toString().equalsIgnoreCase(hearingId)
            );

        } catch (Exception e) {
            throw new AssertionError("HearingStub.verifyPostCourtHearing failed with: " + e);
        }
    }

    private static Optional<JSONObject> getListCourtHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(HEARING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(HEARING_RESPONSE_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new).findFirst();
    }
}
