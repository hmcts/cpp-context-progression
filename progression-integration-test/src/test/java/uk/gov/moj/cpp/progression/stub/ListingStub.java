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
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.json.JSONObject;

public class ListingStub {

    public static final String LISTING_COMMAND = "/listing-service/command/api/rest/listing/cases";
    public static final String LISTING_COMMAND_TYPE = "application/vnd.listing.command.list-court-hearing+json";

    public static void stubListCourtHearing() {
        InternalEndpointMockUtils.stubPingFor("listing-service");

        stubFor(post(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, LISTING_COMMAND_TYPE)));

        stubFor(get(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(LISTING_COMMAND, LISTING_COMMAND_TYPE);
    }

    public static void verifyPostListCourtHearing(final String caseId, final String defendantId) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                        return prosecutionCase.getString("id").equals(caseId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId);
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearing(final String caseId, final String defendantId, final boolean isYouth) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                        JSONObject defendantListingNeeds = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("defendantListingNeeds").getJSONObject(0);

                                        return prosecutionCase.getString("id").equals(caseId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                defendantListingNeeds.getBoolean("isYouth") == isYouth;
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }


    public static void verifyPostListCourtHearing(final String caseId, final String defendantId, final String offenceId, String applicationId) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    {
                        final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStream();
                        listCourtHearingRequestsAsStream
                                .anyMatch(
                                        payload -> {
                                            JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                            boolean courtApplicationExists = payload.getJSONArray("hearings").getJSONObject(0).has("courtApplications");
                                            boolean courtApplicationPartyListingNeedsExists = payload.getJSONArray("hearings").getJSONObject(0).has("courtApplicationPartyListingNeeds");
                                            JSONObject courtApplication = null;
                                            JSONObject courtApplicationPartyListingNeeds = null;
                                            if (courtApplicationExists) {
                                                courtApplication = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                                courtApplicationPartyListingNeeds = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplicationPartyListingNeeds").getJSONObject(0);
                                            }
                                            if (prosecutionCase.getString("id").equals(caseId) &&
                                                    prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                    prosecutionCase.getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).getString("id").equals(offenceId) &&
                                                    courtApplicationExists &&
                                                    courtApplication.getString("id").equals(applicationId) &&
                                                    courtApplication.getJSONObject("type").getString("applicationJurisdictionType").equals("MAGISTRATES") &&
                                                    courtApplicationPartyListingNeedsExists &&
                                                    courtApplicationPartyListingNeeds.getString("courtApplicationId").equals(applicationId) &&
                                                    courtApplicationPartyListingNeeds.getString("courtApplicationPartyId").equals(applicationId) &&
                                                    courtApplicationPartyListingNeeds.getString("hearingLanguageNeeds").equals("ENGLISH"))
                                                return true;
                                            else return false;


                                        }
                                );
                    }

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearing(final String applicationId) {
        try {
            Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() ->
                    {
                        final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStream();
                        listCourtHearingRequestsAsStream
                                .anyMatch(
                                        payload -> {
                                            JSONObject courtApplication = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                            return courtApplication.getString("id").equals(applicationId);
                                        }
                                );
                    }

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static String getHearingIdFromListCourtHearingRequest() {
        JSONObject requestBody = getListCourtHearingRequestsAsStream().findFirst().get();
        return requestBody.getJSONArray("hearings").getJSONObject(0).getString("id");
    }

    private static Stream<JSONObject> getListCourtHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(LISTING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_COMMAND_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new);
    }
}
