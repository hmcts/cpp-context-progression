package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubCourtScheduleDraftStatusReturnsDraft;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubCourtScheduleDraftStatusReturnsNonDraft;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListCourtHearing;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the unallocated-CROWN courtCentre.roomId/roomName strip behaviour
 * in {@code ListHearingRequestedProcessor.stripCourtCentreRoomIfAnyDraftSession}.
 *
 * <p>Exercises the full chain: HTTP POST to {@code progression.list-new-hearing} →
 * {@code progression.command.list-new-hearing} → HearingAggregate.listNewHearing →
 * {@code progression.event.list-hearing-requested} → ListHearingRequestedProcessor.handle →
 * Requester call to listing-query-api's {@code listing.query.court.schedule.draft.status}
 * (WireMock-stubbed) → onward forwarding to listing-command (WireMock-captured).
 *
 * <p>We verify the strip by inspecting the body of the outgoing
 * {@code listing.command.list-court-hearing} request captured at the WireMock stub. If
 * the strip fires the captured body's courtCentre carries only {@code id} and
 * {@code name}; if it skips, the body keeps {@code roomId} and {@code roomName}. Polling
 * the progression hearing query API for the same assertion would require knowing the
 * generated hearingId, which the ListNewHearingHandler picks at random — verifying
 * via the captured downstream request avoids that coupling.
 */
public class CourtScheduleDraftStatusStripIT extends AbstractIT {

    private static final String LISTING_COMMAND_PATH = "/listing-service/command/api/rest/listing/cases";
    private static final String LISTING_COMMAND_TYPE = "application/vnd.listing.command.list-court-hearing+json";
    private static final String PROGRESSION_LIST_NEW_HEARING_TYPE = "application/vnd.progression.list-new-hearing+json";

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();
    }

    @Test
    public void shouldStripCourtCentreRoomWhenListingDraftStatusReportsDraft() throws Exception {
        // Given a CROWN case and a hearing
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // And listing-query-api reports the booked session is DRAFT
        stubCourtScheduleDraftStatusReturnsDraft();
        stubListCourtHearing();

        // When a CROWN list-new-hearing arrives with bookedSlots AND a populated courtCentre.roomId
        final String courtScheduleId = randomUUID().toString();
        final String courtCentreId = "89592405-c29b-3706-b1d3-b1dd3a08b227";
        final String roomId = "d0624ee3-9198-3c8b-94d6-42fb197ebe5e";
        postCommand(getWriteUrl("/listnewhearing"),
                PROGRESSION_LIST_NEW_HEARING_TYPE,
                buildCrownListNewHearingPayload(caseId, defendantId, courtScheduleId, courtCentreId, roomId));

        // Then the body forwarded to listing-command has courtCentre WITHOUT roomId / roomName
        verifyListCourtHearingHasCourtCentreWithoutRoom(caseId);
    }

    @Test
    public void shouldPreserveCourtCentreRoomWhenListingDraftStatusReportsNonDraft() throws Exception {
        // Given a CROWN case and a hearing
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // And listing-query-api reports the booked session is NOT draft
        stubCourtScheduleDraftStatusReturnsNonDraft();
        stubListCourtHearing();

        // When a CROWN list-new-hearing arrives with bookedSlots AND a populated courtCentre.roomId
        final String courtScheduleId = randomUUID().toString();
        final String courtCentreId = "89592405-c29b-3706-b1d3-b1dd3a08b227";
        final String roomId = "d0624ee3-9198-3c8b-94d6-42fb197ebe5e";
        postCommand(getWriteUrl("/listnewhearing"),
                PROGRESSION_LIST_NEW_HEARING_TYPE,
                buildCrownListNewHearingPayload(caseId, defendantId, courtScheduleId, courtCentreId, roomId));

        // Then the body forwarded to listing-command preserves courtCentre.roomId / roomName
        verifyListCourtHearingPreservesCourtCentreRoom(caseId, roomId);
    }

    private static String buildCrownListNewHearingPayload(final String caseId,
                                                          final String defendantId,
                                                          final String courtScheduleId,
                                                          final String courtCentreId,
                                                          final String roomId) {
        return "{"
                + "\"listNewHearing\":{"
                + "\"hearingType\":{\"id\":\"fb90d7d1-a591-4deb-92a0-4e3d0d469bce\",\"description\":\"Trial - no witnesses\"},"
                + "\"estimatedMinutes\":120,"
                + "\"earliestStartDateTime\":\"2026-06-15T09:00:00.000Z\","
                + "\"jurisdictionType\":\"CROWN\","
                + "\"bookedSlots\":[{"
                + "  \"startTime\":\"2026-06-15T09:00:00.000Z\","
                + "  \"duration\":120,"
                + "  \"courtScheduleId\":\"" + courtScheduleId + "\","
                + "  \"session\":\"AD\","
                + "  \"oucode\":\"C01BL00\","
                + "  \"courtRoomId\":235,"
                + "  \"courtCentreId\":\"" + courtCentreId + "\","
                + "  \"roomId\":\"" + roomId + "\""
                + "}],"
                + "\"courtCentre\":{"
                + "  \"id\":\"" + courtCentreId + "\","
                + "  \"name\":\"Blackfriars Crown Court\","
                + "  \"roomId\":\"" + roomId + "\","
                + "  \"roomName\":\"Courtroom 01\""
                + "},"
                + "\"listDefendantRequests\":[{"
                + "  \"prosecutionCaseId\":\"" + caseId + "\","
                + "  \"defendantId\":\"" + defendantId + "\","
                + "  \"defendantOffences\":[\"" + randomUUID() + "\"]"
                + "}]"
                + "},"
                + "\"sendNotificationToParties\":false"
                + "}";
    }

    private static void verifyListCourtHearingHasCourtCentreWithoutRoom(final String caseId) {
        try {
            waitAtMost(ofSeconds(30)).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
                    findAll(postRequestedFor(urlPathEqualTo(LISTING_COMMAND_PATH))
                            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(LISTING_COMMAND_TYPE)))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .anyMatch(body -> isListCourtHearingForCaseWithStrippedCourtCentre(body, caseId)));
        } catch (Exception e) {
            fail("listing-command body for case " + caseId + " did not arrive with courtCentre.roomId stripped: " + e.getMessage());
        }
    }

    private static void verifyListCourtHearingPreservesCourtCentreRoom(final String caseId, final String expectedRoomId) {
        try {
            waitAtMost(ofSeconds(30)).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
                    findAll(postRequestedFor(urlPathEqualTo(LISTING_COMMAND_PATH))
                            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(LISTING_COMMAND_TYPE)))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .anyMatch(body -> isListCourtHearingForCaseWithPreservedRoom(body, caseId, expectedRoomId)));
        } catch (Exception e) {
            fail("listing-command body for case " + caseId + " did not arrive with courtCentre.roomId preserved as " + expectedRoomId + ": " + e.getMessage());
        }
    }

    private static boolean isListCourtHearingForCaseWithStrippedCourtCentre(final String body, final String caseId) {
        try {
            final JSONObject payload = new JSONObject(body);
            if (!payload.has("hearings") || payload.getJSONArray("hearings").length() == 0) {
                return false;
            }
            final JSONObject hearing = payload.getJSONArray("hearings").getJSONObject(0);
            if (!referencesCase(hearing, caseId)) {
                return false;
            }
            // Strip succeeded when the courtCentre block omits roomId AND roomName entirely
            // (or has them present as JSON null). bookedSlots[].roomId is intentionally NOT
            // stripped — only the denormalised courtCentre fields are.
            final JSONObject courtCentre = hearing.optJSONObject("courtCentre");
            if (courtCentre == null) {
                return true;
            }
            return courtCentre.isNull("roomId") && courtCentre.isNull("roomName");
        } catch (JSONException e) {
            return false;
        }
    }

    private static boolean isListCourtHearingForCaseWithPreservedRoom(final String body, final String caseId, final String expectedRoomId) {
        try {
            final JSONObject payload = new JSONObject(body);
            if (!payload.has("hearings") || payload.getJSONArray("hearings").length() == 0) {
                return false;
            }
            final JSONObject hearing = payload.getJSONArray("hearings").getJSONObject(0);
            if (!referencesCase(hearing, caseId)) {
                return false;
            }
            final JSONObject courtCentre = hearing.optJSONObject("courtCentre");
            return courtCentre != null
                    && expectedRoomId.equals(courtCentre.optString("roomId"));
        } catch (JSONException e) {
            return false;
        }
    }

    private static boolean referencesCase(final JSONObject hearing, final String caseId) throws JSONException {
        if (!hearing.has("prosecutionCases") || hearing.getJSONArray("prosecutionCases").length() == 0) {
            return false;
        }
        return caseId.equals(hearing.getJSONArray("prosecutionCases").getJSONObject(0).getString("id"));
    }
}
