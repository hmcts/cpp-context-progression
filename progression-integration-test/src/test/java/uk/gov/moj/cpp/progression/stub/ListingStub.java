package uk.gov.moj.cpp.progression.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.json.JSONObject;

public class ListingStub {

    public static final String LISTING_COMMAND = "/listing-service/command/api/rest/listing/cases";
    public static final String LISTING_COMMAND_TYPE = "application/vnd.listing.command.send-case-for-listing+json";

    public static void stubSendCaseForListing() {
        InternalEndpointMockUtils.stubPingFor("listing-service");

        stubFor(post(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, LISTING_COMMAND_TYPE)));

        stubFor(get(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(LISTING_COMMAND, LISTING_COMMAND_TYPE);
    }

    public static void verifyPostSendCaseForListing(final String caseId, final String defendantId) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    getSendCasesForListingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                        return prosecutionCase.getString("id").equals(caseId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId);
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostSendCaseForListing failed with: " + e);
        }
    }

    public static void verifyPostSendCaseForListing(final String caseId, final String defendantId, final String offenceId) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    getSendCasesForListingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                        return prosecutionCase.getString("id").equals(caseId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).getString("id").equals(offenceId);
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostSendCaseForListing failed with: " + e);
        }
    }

    public static String getHearingIdFromSendCaseForListingRequest(){
        JSONObject requestBody = getSendCasesForListingRequestsAsStream().findFirst().get();
        return requestBody.getJSONArray("hearings").getJSONObject(0).getString("id");
    }

    private static Stream<JSONObject> getSendCasesForListingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(LISTING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_COMMAND_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new);
    }
}
