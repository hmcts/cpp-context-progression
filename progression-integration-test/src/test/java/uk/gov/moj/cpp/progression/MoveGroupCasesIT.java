package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonString;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.json.JSONException;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForCaseHearings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForGroupCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.moveGroupCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollGroupCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingForGroupCase;

/**
 * Covers the two handler-level branches that depend on real downstream infrastructure (the
 * viewstore-backed group/case queries, the new hearing's actual creation, and the Listing
 * notification) rather than being fully exercisable with mocks in
 * {@code MoveGroupCasesHandlerTest}. Validation-only branches (the three
 * {@code IllegalArgumentException}s, the old-master-promotion fix) are already covered there and
 * not repeated here.
 */
public class MoveGroupCasesIT extends AbstractIT {

    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String groupId;
    private String courtCentreId;
    private String courtCentreName;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        groupId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtCentreName = "Croydon Magistrate's Court";
    }

    @Test
    public void shouldMoveSubsetOfCasesIntoNewGroup_AndNotifyListingOfTheNewHearing() throws IOException, JSONException {
        final int caseCount = 3;
        final Map<UUID, Pair<UUID, UUID>> caseDefendantOffence = Stream.generate(UUID::randomUUID).limit(caseCount)
                .collect(Collectors.toMap(caseId -> caseId, caseId -> new Pair<>(randomUUID(), randomUUID())));
        final List<UUID> caseIds = new ArrayList<>(caseDefendantOffence.keySet());
        final UUID masterCaseId = caseIds.get(0);
        final UUID movedCaseId1 = caseIds.get(1);
        final UUID movedCaseId2 = caseIds.get(2);

        initiateCourtProceedingsForGroupCases(masterCaseId, caseDefendantOffence, listedStartDateTime, earliestStartDateTime, groupId, courtCentreId, courtCentreName);
        final String oldHearingId = verifyPostListCourtHearing(masterCaseId, caseCount);

        final UUID newGroupId = randomUUID();
        final List<UUID> movedCaseIds = List.of(movedCaseId1, movedCaseId2);

        final JmsMessageConsumerClient publicGroupCreatedConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("public.progression.group-created-from-moved-cases").getMessageConsumerClient();

        moveGroupCases(fromString(groupId), null, newGroupId, movedCaseIds, movedCaseId1);

        // the confirmation a caller waits for before showing a success screen — sent as soon as the
        // move itself succeeds, independent of (and before) the hearing-mirroring work below
        final Optional<JsonObject> groupCreatedMessage = retrieveMessageBody(publicGroupCreatedConsumer);
        assertTrue(groupCreatedMessage.isPresent());
        final JsonObject groupCreatedEvent = groupCreatedMessage.get();
        assertThat(groupCreatedEvent.getString("groupId"), is(newGroupId.toString()));
        assertThat(groupCreatedEvent.getJsonObject("leadCase").getString("id"), is(movedCaseId1.toString()));
        final List<String> confirmedCaseIds = groupCreatedEvent.getJsonArray("caseIds").getValuesAs(JsonString::getString);
        assertThat(confirmedCaseIds, containsInAnyOrder(movedCaseId1.toString(), movedCaseId2.toString()));

        // old group: only the untouched master remains
        pollGroupCases(groupId,
                withJsonPath("$.cases.length()", is(1)),
                withJsonPath("$.leadCase.caseId", is(masterCaseId.toString())));

        // new group: both moved cases, new lead case correctly flagged
        pollGroupCases(newGroupId.toString(),
                withJsonPath("$.cases.length()", is(2)),
                withJsonPath("$.leadCase.caseId", is(movedCaseId1.toString())));

        // each moved case's own progression view reflects the new group association
        pollProsecutionCasesProgressionFor(movedCaseId1.toString(),
                withJsonPath("$.prosecutionCase.groupId", is(newGroupId.toString())),
                withJsonPath("$.prosecutionCase.isGroupMember", is(true)),
                withJsonPath("$.prosecutionCase.isGroupMaster", is(true)));
        pollProsecutionCasesProgressionFor(movedCaseId2.toString(),
                withJsonPath("$.prosecutionCase.groupId", is(newGroupId.toString())),
                withJsonPath("$.prosecutionCase.isGroupMember", is(true)),
                withJsonPath("$.prosecutionCase.isGroupMaster", is(false)));

        // the case left behind in the old group is unaffected
        pollProsecutionCasesProgressionFor(masterCaseId.toString(),
                withJsonPath("$.prosecutionCase.groupId", is(groupId)),
                withJsonPath("$.prosecutionCase.isGroupMaster", is(true)));

        // a brand new hearing was created for the new group, and Listing was actually notified
        // about it (this is the fix: GroupCreatedFromMovedCasesProcessor must populate
        // listHearingRequests, otherwise ProsecutionCaseDefendantListingStatusChangedProcessor's
        // own isEmpty(listHearingRequests) guard silently skips the outbound call entirely)
        final String newHearingPayload = verifyPostListCourtHearingForGroupCase(movedCaseId1.toString());
        final JsonObject newHearingListing = stringToJsonObjectConverter.convert(newHearingPayload)
                .getJsonArray("hearings").getJsonObject(0);
        final String newHearingId = newHearingListing.getString("id");
        assertThat(newHearingId, is(not(oldHearingId)));

        final List<String> newHearingCaseIds = newHearingListing.getJsonArray("prosecutionCases").stream()
                .map(value -> value.asJsonObject().getString("id"))
                .collect(Collectors.toList());
        assertThat(newHearingCaseIds, containsInAnyOrder(movedCaseId1.toString(), movedCaseId2.toString()));

        // both moved cases' own "current hearing" pointer now points at the new hearing, not the old one
        pollForCaseHearings(movedCaseId1.toString(),
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].hearingId", is(not(oldHearingId))));
        pollForCaseHearings(movedCaseId2.toString(),
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].hearingId", is(not(oldHearingId))));
    }

    @Test
    public void shouldRejectMove_WhenItWouldLeaveOldGroupWithZeroMembers() throws IOException, JSONException {
        final int caseCount = 2;
        final Map<UUID, Pair<UUID, UUID>> caseDefendantOffence = Stream.generate(UUID::randomUUID).limit(caseCount)
                .collect(Collectors.toMap(caseId -> caseId, caseId -> new Pair<>(randomUUID(), randomUUID())));
        final List<UUID> caseIds = new ArrayList<>(caseDefendantOffence.keySet());
        final UUID masterCaseId = caseIds.get(0);
        final UUID otherCaseId = caseIds.get(1);

        initiateCourtProceedingsForGroupCases(masterCaseId, caseDefendantOffence, listedStartDateTime, earliestStartDateTime, groupId, courtCentreId, courtCentreName);
        verifyPostListCourtHearing(masterCaseId, caseCount);

        final JmsMessageConsumerClient publicRejectedConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("public.progression.move-last-cases-from-group-rejected").getMessageConsumerClient();

        // moving every member out (including the master, hence a non-null currentGroup.newLeadCaseId
        // to satisfy validation) would leave the old group with zero members — rejected before anything else changes
        moveGroupCases(fromString(groupId), otherCaseId, randomUUID(), List.of(masterCaseId, otherCaseId), masterCaseId);

        final Optional<JsonObject> message = retrieveMessageBody(publicRejectedConsumer);
        assertTrue(message.isPresent());
        final JsonObject rejectedEvent = message.get();
        assertThat(rejectedEvent.getString("groupId"), is(groupId));

        // old group unchanged
        pollGroupCases(groupId, withJsonPath("$.cases.length()", is(2)));
    }

    private String verifyPostListCourtHearing(final UUID caseId, final int numberOfGroupCases) {
        final String payload = verifyPostListCourtHearingForGroupCase(caseId.toString());
        final JsonObject listCourtHearingPayload = stringToJsonObjectConverter.convert(payload);
        assertThat(listCourtHearingPayload.getJsonArray("hearings").size(), is(1));
        final JsonObject hearing = listCourtHearingPayload.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getInt("numberOfGroupCases"), is(numberOfGroupCases));
        assertThat(hearing.getJsonArray("prosecutionCases").size(), is(1));
        assertThat(hearing.containsKey("id"), is(true));
        return hearing.getString("id");
    }
}
