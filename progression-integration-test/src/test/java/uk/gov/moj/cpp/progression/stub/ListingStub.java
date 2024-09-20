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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.text.MessageFormat.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import java.time.Duration;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.waitAtMost;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;

public class ListingStub {

    private static final String LISTING_COMMAND = "/listing-service/command/api/rest/listing/cases";
    private static final String LISTING_HEARING_COMMAND_V2 = "/listing-service/command/api/rest/listing/hearings/.*";
    private static final String LISTING_DELETE_HEARING_COMMAND = "/listing-command-api/command/api/rest/listing/delete-hearing/";
    private static final String LISTING_COMMAND_TYPE = "application/vnd.listing.command.list-court-hearing+json";

    private static final String LISTING_UNSCHEDULED_HEARING_COMMAND_TYPE = "application/vnd.listing.command.list-unscheduled-court-hearing+json";

    private static final String LISTING_UNSCHEDULED_HEARING_COMMAND_TYPE_V2 = "application/vnd.listing.list-unscheduled-next-hearings+json";

    private static final String LISTING_ANY_ALLOCATION_HEARING_QUERY_TYPE = "application/vnd.listing.search.hearings+json";
    private static final String LISTING_NEXT_HEARING_V2_TYPE = "application/vnd.listing.next-hearings-v2+json";
    private static final String LISTING_COTR_SEARCH_QUERY_TYPE = "application/vnd.listing.search.hearings+json";
    public static final String LISTING_DELETE_NEXT_HEARINGS = "listing.delete-next-hearings";
    private static final String LISTING_DELETE_HEARING_TYPE = "application/vnd.listing.delete-hearing+json";
    private static final String LISTING_DELETE_NEXT_HEARINGS_TYPE = "application/vnd.listing.delete-next-hearings+json";

    public static void stubListCourtHearing() {
        InternalEndpointMockUtils.stubPingFor("listing-service");

        stubFor(post(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, LISTING_COMMAND_TYPE)));

        stubFor(post(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, LISTING_UNSCHEDULED_HEARING_COMMAND_TYPE)));

        stubFor(post(urlPathMatching(LISTING_HEARING_COMMAND_V2))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_NEXT_HEARING_V2_TYPE))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())));

        stubFor(post(urlPathMatching(LISTING_HEARING_COMMAND_V2))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_UNSCHEDULED_HEARING_COMMAND_TYPE_V2))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())));

        stubFor(post(urlPathMatching(LISTING_HEARING_COMMAND_V2))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_DELETE_NEXT_HEARINGS))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())));

        stubFor(post(urlPathMatching(LISTING_DELETE_HEARING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_DELETE_HEARING_TYPE))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())));

        stubFor(post(urlPathMatching(LISTING_HEARING_COMMAND_V2))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_DELETE_NEXT_HEARINGS_TYPE))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())));

        stubFor(get(urlPathEqualTo(LISTING_COMMAND))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(LISTING_COMMAND, LISTING_COMMAND_TYPE);
    }

    public static void verifyPostListCourtHearing(final String caseId, final String defendantId) {
        try {
            waitAtMost(ofMinutes(1)).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        try {
                                            if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("prosecutionCases")) {
                                                JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                                return prosecutionCase.getString("id").equals(caseId) &&
                                                        prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId);
                                            } else {
                                                return false;
                                            }
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearing(final String caseId, final String defendantId, final String courtScheduleId) {
        try {
            waitAtMost(ofMinutes(1)).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        try {
                                            if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("prosecutionCases") &&
                                                    payload.getJSONArray("hearings").getJSONObject(0).has("bookedSlots") &&
                                                    payload.getJSONArray("hearings").getJSONObject(0).has("bookingType") &&
                                                    payload.getJSONArray("hearings").getJSONObject(0).has("priority") &&
                                                    payload.getJSONArray("hearings").getJSONObject(0).has("specialRequirements") &&
                                                    payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("specialRequirements").getString(0).equals("RSZ") &&
                                                    payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("specialRequirements").getString(1).equals("CELL")
                                            ) {
                                                JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                                String id = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("bookedSlots").getJSONObject(0).getString("courtScheduleId");
                                                return prosecutionCase.getString("id").equals(caseId) &&
                                                        prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                        courtScheduleId.equals(id);
                                            } else {
                                                return false;
                                            }
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static String verifyPostListCourtHearingForGroupCase(final String containsText) {
        try {
            return waitAtMost(Duration.ofMinutes(1)).until(() -> {
                        final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStream();
                        return listCourtHearingRequestsAsStream
                                .filter(payload -> {
                                    try {
                                        return payload.has("hearings") &&
                                                payload.getJSONArray("hearings").getJSONObject(0).has("prosecutionCases");
                                    }catch (JSONException e) {
                                        return false;
                                    }
                                })
                                .map(JSONObject::toString)
                                .filter(s -> s.contains(containsText))
                                .findFirst()
                                .orElse("{}");
                    }, JsonPathMatchers.hasJsonPath("$.hearings")
            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearing(final String caseId, final String defendantId, final boolean isYouth) {
        try {
            waitAtMost(ofMinutes(1)).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        try {
                                            if (payload.getJSONArray("hearings").getJSONObject(0).has("prosecutionCases")) {
                                                JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                                JSONObject defendantListingNeeds = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("defendantListingNeeds").getJSONObject(0);

                                                return prosecutionCase.getString("id").equals(caseId) &&
                                                        prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                        defendantListingNeeds.getBoolean("isYouth") == isYouth;

                                            } else {
                                                return false;
                                            }
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearingWithCommittingCourt(final String caseId, final String defendantId, final String courtHouseType) {
        try {
            waitAtMost(ofMinutes(1)).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        try {
                                            if (payload.getJSONArray("hearings").getJSONObject(0).has("prosecutionCases")) {
                                                final JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                                final JSONObject defendants = prosecutionCase.getJSONArray("defendants").getJSONObject(0);
                                                final JSONObject offences = defendants.getJSONArray("offences").getJSONObject(0);

                                                return prosecutionCase.getString("id").equals(caseId) &&
                                                        defendants.getString("id").equals(defendantId) &&
                                                        offences.getJSONObject("committingCourt").getString("courtHouseType").equals(courtHouseType);
                                            } else {
                                                return false;
                                            }
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            )

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }


    public static void verifyPostListCourtHearing(final String caseId, final String defendantId, final String offenceId, String applicationId) {
        try {
            waitAtMost(ofSeconds(10)).until(() ->
                    getListCourtHearingRequestsAsStream().anyMatch(
                            payload -> {
                                if (payload.has("hearings")) {
                                    JSONObject prosecutionCase = null;
                                    try {
                                        prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                    } catch (JSONException e) {
                                        return false;
                                    }
                                    boolean courtApplicationExists = false;
                                    try {
                                        courtApplicationExists = payload.getJSONArray("hearings").getJSONObject(0).has("courtApplications");
                                    } catch (JSONException e) {
                                        return false;
                                    }
                                    boolean courtApplicationPartyListingNeedsExists = false;
                                    try {
                                        courtApplicationPartyListingNeedsExists = payload.getJSONArray("hearings").getJSONObject(0).has("courtApplicationPartyListingNeeds");
                                    } catch (JSONException e) {
                                        return false;
                                    }
                                    JSONObject courtApplication = null;
                                    JSONObject courtApplicationPartyListingNeeds = null;
                                    if (courtApplicationExists) {
                                        try {
                                            courtApplication = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                        try {
                                            courtApplicationPartyListingNeeds = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplicationPartyListingNeeds").getJSONObject(0);
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                                    try {
                                        return prosecutionCase.getString("id").equals(caseId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                prosecutionCase.getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).getString("id").equals(offenceId) &&
                                                courtApplicationExists &&
                                                courtApplication.getString("id").equals(applicationId) &&
                                                courtApplication.getJSONObject("type").getString("applicationJurisdictionType").equals("MAGISTRATES") &&
                                                courtApplicationPartyListingNeedsExists &&
                                                courtApplicationPartyListingNeeds.getString("courtApplicationId").equals(applicationId) &&
                                                courtApplicationPartyListingNeeds.getString("courtApplicationPartyId").equals(applicationId) &&
                                                courtApplicationPartyListingNeeds.getString("hearingLanguageNeeds").equals("ENGLISH");
                                    } catch (JSONException e) {
                                        return false;
                                    }
                                } else {
                                    return false;
                                }

                            }
                    )
            );

        } catch (
                Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }

    }

    public static void verifyPostListCourtHearing(final String applicationId) {
        try {
            waitAtMost(ofSeconds(10)).until(() ->
                    getListCourtHearingRequestsAsStream()
                            .anyMatch(
                                    payload -> {
                                        try {
                                            if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("courtApplications")) {
                                                JSONObject courtApplication = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                                return courtApplication.getString("id").equals(applicationId)
                                                        && payload.getJSONArray("hearings").getJSONObject(0).has("bookingType")
                                                        && payload.getJSONArray("hearings").getJSONObject(0).has("priority")
                                                        && payload.getJSONArray("hearings").getJSONObject(0).has("specialRequirements")
                                                        && payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("specialRequirements").getString(0).equals("RSZ")
                                                        && payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("specialRequirements").getString(1).equals("CELL");
                                            } else {
                                                return false;
                                            }
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            )
            );
        } catch (
                Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearingV2(final String applicationId) {
        try {
            waitAtMost(ofSeconds(10)).until(() ->
                    getListCourtHearingRequestsAsStreamV2()
                            .anyMatch(
                                    payload -> {
                                        try {
                                            if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("courtApplications")) {
                                                JSONObject courtApplication = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                                return courtApplication.getString("id").equals(applicationId);
                                            } else {
                                                return false;
                                            }
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            )
            );
        } catch (
                Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearingV2ForHmiSlots() {
        try {
            waitAtMost(ofSeconds(10)).until(() ->
                    getListCourtHearingRequestsAsStreamV2()
                            .anyMatch(payload -> payload.toString().contains("bookedSlots")));
        } catch (
                Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearingV2ForHmiSlots failed with: " + e);
        }
    }

    public static void verifyPostListCourtHearingV2() {
        try {
            waitAtMost(ofSeconds(10)).until(() ->
                    getListCourtHearingRequestsAsStreamV2()
                            .anyMatch(
                                    payload -> payload.has("hearings")
                            )
            );
        } catch (
                Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearing failed with: " + e);
        }
    }

    public static void verifyListNextHearingRequestsAsStreamV2(final String hearingId, final String estimatedDuration) {
        waitAtMost(ofSeconds(10)).until(() -> {
                    final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStreamV2();
                    return listCourtHearingRequestsAsStream.anyMatch(
                            payload -> {
                                try {
                                    if (payload.has("hearings") &&
                                            payload.getJSONArray("hearings").getJSONObject(0).has("estimatedDuration")) {
                                        final JSONObject hearing = payload.getJSONArray("hearings").getJSONObject(0);
                                        final String seedingHearingId = payload.getJSONObject("seedingHearing").getString("seedingHearingId");
                                        final String estimatedDurationPayload = payload.getJSONArray("hearings").getJSONObject(0).getString("estimatedDuration");
                                        return seedingHearingId.equals(hearingId) && estimatedDurationPayload.equals(estimatedDuration);
                                    } else {
                                        return false;
                                    }
                                } catch (JSONException e) {
                                    return false;
                                }
                            }
                    );
                }
        );
    }

    public static void verifyListNextHearingRequestsAsStream(final String hearingId, final String estimatedDuration) {
        waitAtMost(ofSeconds(10)).until(() -> {
                    final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStream();
                    return listCourtHearingRequestsAsStream.anyMatch(
                            payload -> {
                                try {
                                    if (payload.has("hearings") &&
                                            payload.getJSONArray("hearings").getJSONObject(0).has("estimatedDuration")) {
                                        final JSONObject hearing = payload.getJSONArray("hearings").getJSONObject(0);
                                        final String seedingHearingId = payload.getJSONObject("seedingHearing").getString("seedingHearingId");
                                        final String estimatedDurationPayload = payload.getJSONArray("hearings").getJSONObject(0).getString("estimatedDuration");
                                        return seedingHearingId.equals(hearingId) && estimatedDurationPayload.equals(estimatedDuration);
                                    } else {
                                        return false;
                                    }
                                } catch (JSONException e) {
                                    return false;
                                }
                            }
                    );
                }
        );
    }

    public static String getPostListCourtHearing(final String applicationId) {
        try {
            return waitAtMost(ofSeconds(10)).until(() ->
                    {
                        final Stream<JSONObject> listCourtHearingRequestsAsStream = getListCourtHearingRequestsAsStream();
                        return listCourtHearingRequestsAsStream
                                .filter(
                                        payload -> {
                                            try {
                                                if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("courtApplications")) {
                                                    JSONObject courtApplication = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                                    return courtApplication.getString("id").equals(applicationId);
                                                } else {
                                                    return false;
                                                }
                                            } catch (JSONException e) {
                                                return false;
                                            }
                                        }
                                ).findFirst().map(JSONObject::toString).orElse("{hearings:[]");
                    }, JsonPathMatchers.hasJsonPath("$.hearings")

            );
        } catch (
                Exception e) {
            throw new AssertionError("ListingStub.getPostListCourtHearing failed with: " + e);
        }
    }


    public static void verifyListUnscheduledHearingRequestsAsStreamV2(final String hearingId,
                                                                      final String estimatedDuration) {
        waitAtMost(ofSeconds(10)).until(() -> {
                    final Stream<JSONObject> listCourtHearingRequestsAsStream = getListUnscheduledHearingRequestsAsStreamV2();
                    return listCourtHearingRequestsAsStream.anyMatch(
                            payload -> {
                                try {
                                    if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("estimatedDuration")) {
                                        final JSONObject hearing = payload.getJSONArray("hearings").getJSONObject(0);
                                        return hearing.getString("id").equals(hearingId) &&
                                                payload.getJSONArray("hearings").getJSONObject(0).getString("estimatedDuration").equals(estimatedDuration);
                                    } else {
                                        return false;
                                    }
                                } catch (JSONException e) {
                                    return false;
                                }
                            }
                    );
                }
        );

    }

    public static void verifyListUnscheduledHearingRequestsAsStream(final String hearingId,
                                                                    final String estimatedDuration) {
        waitAtMost(ofSeconds(10)).until(() -> {
                    final Stream<JSONObject> listCourtHearingRequestsAsStream = getListUnscheduledHearingRequestsAsStream();
                    return listCourtHearingRequestsAsStream.anyMatch(
                            payload -> {
                                try {
                                    if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("estimatedDuration")) {
                                        final JSONObject hearing = payload.getJSONArray("hearings").getJSONObject(0);
                                        return hearing.getString("id").equals(hearingId) &&
                                                payload.getJSONArray("hearings").getJSONObject(0).getString("estimatedDuration").equals(estimatedDuration);
                                    } else {
                                        return false;
                                    }
                                } catch (JSONException e) {
                                    return false;
                                }
                            }
                    );

                }
        );

    }

    private static Stream<JSONObject> getListUnscheduledHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(LISTING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_UNSCHEDULED_HEARING_COMMAND_TYPE)))
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

    private static Stream<JSONObject> getListUnscheduledHearingRequestsAsStreamV2() {
        return findAll(postRequestedFor(urlMatching(LISTING_HEARING_COMMAND_V2))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_UNSCHEDULED_HEARING_COMMAND_TYPE_V2)))
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

    private static Stream<JSONObject> getListCourtHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(LISTING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_COMMAND_TYPE)))
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

    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.list-court-hearing";

    private static Stream<JSONObject> getListCourtHearingRequestsAsStreamV2() {
        return findAll(postRequestedFor(urlMatching(LISTING_HEARING_COMMAND_V2))
                .withHeader(CONTENT_TYPE, equalTo(LISTING_NEXT_HEARING_V2_TYPE)))
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

    public static void setupListingAnyAllocationQuery(final String caseUrn, String resource) {
        stubPingFor("hearing-service");

        final String urlPath = format("/listing-service/query/api/rest/listing/{0}", caseUrn);
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource))));

        waitForStubToBeReady(urlPath, LISTING_ANY_ALLOCATION_HEARING_QUERY_TYPE);
    }

    public static void setupListingAnyFutureAllocationQuery(final String resource, final ZonedDateTime startDateTime ) {
        stubPingFor("listing-service");

        final String urlPath = "/listing-service/query/api/rest/listing/hearings/any-allocation";
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource).replaceAll("START_TIME",startDateTime.toString()))));

        waitForStubToBeReady(urlPath, "application/vnd.listing.search.hearings+json");
    }

    public static void stubListingSearchHearingsQuery(final String resource,
                                                      final String hearingId) {

        stubPingFor("listing-service");

        final String urlPath = format("/listing-service/query/api/rest/listing/hearings/any-allocation");
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource).replaceAll("HEARING_ID", hearingId))));

        waitForStubToBeReady(urlPath, LISTING_ANY_ALLOCATION_HEARING_QUERY_TYPE);
    }

    public static void stubListingCotrSearch(final String resource, final String hearingId) {

        stubPingFor("listing-service");

        final String urlPath = format("/listing-service/query/api/rest/listing/hearings/cotr-search");
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource).replaceAll("HEARING_ID", hearingId))));

        waitForStubToBeReady(urlPath, LISTING_COTR_SEARCH_QUERY_TYPE);
    }

    public static void verifyPostListCourtHearingWithProsecutorInfo(final String caseId, final String defendantId, final String courtScheduleId) {
        try {
            waitAtMost(Duration.ofMinutes(2)).until(() -> {
                        return getListCourtHearingRequestsAsStream()
                                .anyMatch(
                                        payload -> {
                                            try {
                                                if (payload.has("hearings") && payload.getJSONArray("hearings").getJSONObject(0).has("prosecutionCases") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).has("bookedSlots") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).has("bookingType") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).has("priority") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).has("specialRequirements") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("specialRequirements").getString(0).equals("RSZ") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("specialRequirements").getString(1).equals("CELL") &&
                                                        payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0).has("prosecutor")
                                                ) {
                                                    JSONObject prosecutionCase = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("prosecutionCases").getJSONObject(0);
                                                    JSONObject prosecutor = prosecutionCase.getJSONObject("prosecutor");
                                                    String id = payload.getJSONArray("hearings").getJSONObject(0).getJSONArray("bookedSlots").getJSONObject(0).getString("courtScheduleId");
                                                    return prosecutionCase.getString("id").equals(caseId) &&
                                                            prosecutionCase.getJSONArray("defendants").getJSONObject(0).getString("id").equals(defendantId) &&
                                                            courtScheduleId.equals(id) && prosecutor.getString("prosecutorCode").equals("CPS-EM");
                                                } else {
                                                    return false;
                                                }
                                            }catch (JSONException e) {
                                                return false;
                                            }
                                        }
                                );
                    }

            );

        } catch (Exception e) {
            throw new AssertionError("ListingStub.verifyPostListCourtHearingWithProsecutorInfo failed with: " + e);
        }
    }
}
