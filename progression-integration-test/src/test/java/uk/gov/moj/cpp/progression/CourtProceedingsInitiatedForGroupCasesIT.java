package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.civilCaseInitiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForGroupCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollGroupMemberCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.removeCaseFromGroupCases;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.HearingStub.verifyPostInitiateCourtHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.util.Pair;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("squid:S1607")
@ExtendWith(JmsResourceManagementExtension.class)
public class CourtProceedingsInitiatedForGroupCasesIT extends AbstractIT {
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    public static final String PROGRESSION_QUERY_GET_CASE_HEARINGS = "application/vnd.progression.query.casehearings+json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    private JmsMessageConsumerClient publicCourtProceedingsInitiatedEventConsumer;
    private JmsMessageConsumerClient publicCaseRemovedEventConsumer;
    private JmsMessageConsumerClient consumerForCourtProceedingsInitiated;
    private JmsMessageConsumerClient consumerForCaseGroupInfoUpdated;
    private JmsMessageConsumerClient consumerForDefendantListingStatusChanged;
    private JmsMessageConsumerClient consumerForHearingResultedCaseUpdated;
    private JmsMessageConsumerClient consumerForLinkProsecutionCasesToHearing;
    private JmsMessageConsumerClient publicCivilCaseExistsEventConsumer;
    private JmsMessageConsumerClient publicLastCaseRemoveErrorEventConsumer;

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String feesId;
    private String groupId;
    private String courtCentreId;
    private String userId;
    private String courtCentreName;
    private String reportingRestrictionId;

    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        stubInitiateHearing();
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        feesId = randomUUID().toString();
        groupId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        userId = randomUUID().toString();
        publicCourtProceedingsInitiatedEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        consumerForCourtProceedingsInitiated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-proceedings-initiated").getMessageConsumerClient();
        consumerForCaseGroupInfoUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.case-group-info-updated").getMessageConsumerClient();
        publicCaseRemovedEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-removed-from-group-cases").getMessageConsumerClient();
        consumerForDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        courtCentreName = "Croydon Magistrate's Court";
        reportingRestrictionId = randomUUID().toString();
        consumerForHearingResultedCaseUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-resulted-case-updated").getMessageConsumerClient();
        consumerForLinkProsecutionCasesToHearing = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event-link-prosecution-cases-to-hearing").getMessageConsumerClient();
        this.publicCivilCaseExistsEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.events.civil-case-exists").getMessageConsumerClient();
        publicLastCaseRemoveErrorEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.remove-last-case-in-group-cases-rejected").getMessageConsumerClient();
    }


    @Test
    public void shouldInitiateCourtProceedingsForCivilCase() throws IOException {
        //given
        civilCaseInitiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB, feesId);
        //when
        verifyInMessagingQueueForNumberOfTimes(1, publicCourtProceedingsInitiatedEventConsumer);

        final Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());

        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
    }

    @Test
    @Disabled("Flaky tests - passed locally failed at pipeline")
    public void shouldInitiateCourtProceedingsForGroupCases() throws IOException, JSONException {
        final int caseCount = 5;

        final Map<UUID, Pair<UUID, UUID>> caseDefendantOffence = Stream.generate(UUID::randomUUID).limit(caseCount)
                .collect(Collectors.toMap(caseId -> caseId, caseId -> new Pair<>(randomUUID(), randomUUID())));
        final UUID masterCaseId = caseDefendantOffence.keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No case found!"));
        List<String> groupCaseIds = caseDefendantOffence.keySet().stream().map(UUID::toString).collect(Collectors.toList());
        //When
        initiateCourtProceedingsForGroupCases(masterCaseId, caseDefendantOffence, listedStartDateTime, earliestStartDateTime, groupId, courtCentreId, courtCentreName);

        //Then
        verifyInMessagingQueueForNumberOfTimes(0, publicCourtProceedingsInitiatedEventConsumer);
        verifyInMessagingQueueForCourtProceedingsInitiated();
        verifyInMessagingQueueForDefendantListingStatusChangedEvent(caseCount);

        final UUID groupMasterId = verifyCasesAndGetGroupMasterId(caseDefendantOffence.keySet(), emptyList());
        final String hearingId = verifyPostListCourtHearing(groupMasterId, caseCount);
        final UUID masterCaseDefendantId = caseDefendantOffence.get(groupMasterId).getK();
        final UUID masterCaseOffenceId = caseDefendantOffence.get(groupMasterId).getV();

        groupCaseIds.forEach(caseId -> verifyCaseHearings(caseId, hearingId));
        verifyHearing(hearingId, groupCaseIds, "SENT_FOR_LISTING");

        sendPublicListingHearingConfirmedEventForGroupCases(groupMasterId, hearingId, masterCaseDefendantId, masterCaseOffenceId);
        verifyPostInitiateCourtHearing(hearingId);
        verifyHearing(hearingId, groupCaseIds, "HEARING_INITIALISED");

        sendPublicHearingResultedEventForGroupCases(masterCaseId, hearingId, masterCaseDefendantId);
        verifyInMessagingQueueForNumberOfTimes(caseCount, consumerForHearingResultedCaseUpdated);

        final List<UUID> caseIds = new ArrayList<>(caseDefendantOffence.keySet());
        verifyInMessagingQueueForLinkProsecutionCaseToHearingEvent(caseIds, hearingId);

        final UUID caseRemoved = removeNonMasterCaseAndVerifyMasterIsNotChanged(caseDefendantOffence, groupMasterId);
        removeMasterCaseAndVerifyMasterIsChanged(caseDefendantOffence, groupMasterId, caseRemoved);

        final Matcher[] groupIdMatcher = new Matcher[]{
                withJsonPath("$.prosecutionCases[0].groupId", is(groupId))
        };

        pollGroupMemberCases(groupId, groupIdMatcher);
    }

    @Test
    public void shouldTriggerGroupCaseExists() throws IOException, JSONException {
        final int caseCount = 3;

        Map<UUID, Pair<UUID, UUID>> caseDefendantOffence = Stream.generate(UUID::randomUUID).limit(caseCount)
                .collect(Collectors.toMap(caseId -> caseId, caseId -> new Pair<>(randomUUID(), randomUUID())));
        final UUID masterCaseId = caseDefendantOffence.keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No case found!"));
        //When
        initiateCourtProceedingsForGroupCases(masterCaseId, caseDefendantOffence, this.listedStartDateTime, this.earliestStartDateTime, this.groupId, this.courtCentreId, this.courtCentreName);

        //Then
        verifyInMessagingQueueForCourtProceedingsInitiated();
        verifyInMessagingQueueForDefendantListingStatusChangedEvent(caseCount);

        final String newGroupId = randomUUID().toString();
        Map<UUID, Pair<UUID, UUID>> dupCaseDefendantOffence = new HashMap<>();
        dupCaseDefendantOffence.put(masterCaseId, caseDefendantOffence.get(masterCaseId));
        initiateCourtProceedingsForGroupCases(masterCaseId, dupCaseDefendantOffence, this.listedStartDateTime, this.earliestStartDateTime, newGroupId, this.courtCentreId, this.courtCentreName);
        verifyInMessagingQueueForNumberOfTimes(1, this.publicCivilCaseExistsEventConsumer);
    }

    @Test
    public void shouldTriggerErrorForLastCaseFromGroupCaseRemove() throws IOException, JSONException {
        final int caseCount = 2;

        Map<UUID, Pair<UUID, UUID>> caseDefendantOffence = Stream.generate(UUID::randomUUID).limit(caseCount)
                .collect(Collectors.toMap(caseId -> caseId, caseId -> new Pair<>(randomUUID(), randomUUID())));
        final UUID masterCaseId = caseDefendantOffence.keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No case found!"));

        //When
        initiateCourtProceedingsForGroupCases(masterCaseId, caseDefendantOffence, this.listedStartDateTime, this.earliestStartDateTime, this.groupId, this.courtCentreId, this.courtCentreName);

        //Then
        verifyInMessagingQueueForCourtProceedingsInitiated();
        verifyInMessagingQueueForDefendantListingStatusChangedEvent(caseCount);

        removeNonMasterCaseAndVerifyMasterIsNotChanged(caseDefendantOffence, masterCaseId);
        removeCaseFromGroupCases(masterCaseId, fromString(groupId));
        final Optional<JsonObject> message = retrieveMessageBody(publicLastCaseRemoveErrorEventConsumer);

        assertTrue(message.isPresent());
        final JsonObject removeErrorEvent = message.get();
        assertThat(removeErrorEvent.getString("groupId"), is(groupId));
        assertThat(removeErrorEvent.getString("caseId"), is(masterCaseId.toString()));
    }

    private void sendPublicListingHearingConfirmedEventForGroupCases(final UUID masterCaseId, final String hearingId, final UUID defendantId, final UUID offenceId) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed-for-group-cases.json",
                masterCaseId.toString(), hearingId, defendantId.toString(), offenceId.toString(), courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
    }

    private void sendPublicHearingResultedEventForGroupCases(final UUID masterCaseId, final String hearingId, final UUID defendantId) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId), getHearingResultedJsonObject("public.hearing.resulted-masterCase.json", masterCaseId.toString(),
                hearingId, defendantId.toString(), courtCentreId, courtCentreName, reportingRestrictionId, groupId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventEnvelope);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String offenceId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID", offenceId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getHearingResultedJsonObject(final String path, final String caseId, final String hearingId,
                                                    final String defendantId, final String courtCentreId, final String courtCentreName,
                                                    final String reportingRestrictionId, final String groupId) {
        final String payload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("GROUP_ID", groupId)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);

        return stringToJsonObjectConverter.convert(payload);
    }

    private String verifyPostListCourtHearing(final UUID caseId, final int numberOfGroupCases) {
        final String payload = ListingStub.verifyPostListCourtHearingForGroupCase(caseId.toString());
        final JsonObject listCourtHearingPayload = stringToJsonObjectConverter.convert(payload);
        assertThat(listCourtHearingPayload.getJsonArray("hearings").size(), is(1));
        final JsonObject hearing = listCourtHearingPayload.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getInt("numberOfGroupCases"), is(numberOfGroupCases));
        assertThat(hearing.getJsonArray("defendantListingNeeds").size(), is(1));
        assertThat(hearing.getJsonArray("prosecutionCases").size(), is(1));
        assertThat(hearing.containsKey("id"), is(true));
        return hearing.getString("id");
    }

    private UUID removeNonMasterCaseAndVerifyMasterIsNotChanged(final Map<UUID, Pair<UUID, UUID>> caseDefendantOffence, final UUID groupMasterId) throws IOException {
        final UUID caseIdToBeRemoved = caseDefendantOffence.keySet().stream()
                .filter(caseId -> !caseId.equals(groupMasterId))
                .findFirst()
                .orElse(null);

        removeCaseFromGroupCases(caseIdToBeRemoved, fromString(groupId));
        verifyInMQForCaseRemovedFromGroupCases(caseIdToBeRemoved.toString(), groupId, groupMasterId.toString(), false);
        verifyInMQForCaseGroupInfoUpdated(caseIdToBeRemoved.toString(), groupId, false);
        final UUID newGroupMasterId = verifyCasesAndGetGroupMasterId(caseDefendantOffence.keySet(), List.of(caseIdToBeRemoved));
        assertThat(newGroupMasterId, is(groupMasterId));
        return caseIdToBeRemoved;
    }

    private void removeMasterCaseAndVerifyMasterIsChanged(final Map<UUID, Pair<UUID, UUID>> caseDefendantOffence, final UUID groupMasterId, final UUID caseRemoved) throws IOException {
        removeCaseFromGroupCases(groupMasterId, fromString(groupId));
        verifyInMQForCaseRemovedFromGroupCases(groupMasterId.toString(), groupId, groupMasterId.toString(), true);
        verifyInMQForCaseGroupInfoUpdated(groupMasterId.toString(), groupId, true);
        final UUID newGroupMasterId = verifyCasesAndGetGroupMasterId(caseDefendantOffence.keySet(), Arrays.asList(groupMasterId, caseRemoved));
        assertThat(newGroupMasterId, is(not(groupMasterId)));
    }

    private void verifyInMessagingQueueForDefendantListingStatusChangedEvent(int numberOfGroupCases) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForDefendantListingStatusChanged);
        assertTrue(message.isPresent());
        final JsonObject hearing = message.get().getJsonObject("hearing");
        assertThat(hearing, is(notNullValue()));
        assertThat(hearing.getBoolean("isGroupProceedings"), is(true));
        assertThat(hearing.getInt("numberOfGroupCases"), is(numberOfGroupCases));
        assertThat(hearing.getJsonArray("prosecutionCases").size(), is(numberOfGroupCases));
    }

    private void verifyInMessagingQueueForLinkProsecutionCaseToHearingEvent(final List<UUID> caseIds, final String hearingId) {
        caseIds.forEach(caseId -> {
            final JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(consumerForLinkProsecutionCasesToHearing, isJson(Matchers.allOf(
                    withJsonPath("$.hearingId", is(hearingId)),
                    withJsonPath("$.caseId", is(caseId.toString())))));
            assertThat(messageDaysMatchers, notNullValue());
        });
    }

    private void verifyInMessagingQueueForNumberOfTimes(final int times, final JmsMessageConsumerClient messageConsumer) {
        IntStream.range(0, times).forEach(i -> {
                    final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
                    assertTrue(message.isPresent());
                }
        );
    }

    private void verifyInMessagingQueueForCourtProceedingsInitiated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtProceedingsInitiated);
        assertTrue(message.isPresent());
    }

    private void verifyInMQForCaseRemovedFromGroupCases(final String removedCaseId, final String groupId, final String masterCaseId, final boolean groupMasterChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(publicCaseRemovedEventConsumer);

        assertTrue(message.isPresent());

        final JsonObject removedEvent = message.get();
        assertThat(removedEvent.getString("groupId"), is(groupId));
        assertThat(removedEvent.getString("masterCaseId"), is(masterCaseId));

        assertThat(removedEvent.getJsonObject("removedCase").getString("id"), is(removedCaseId));
        assertThat(removedEvent.getJsonObject("removedCase").getString("groupId"), is(groupId));
        assertThat(removedEvent.getJsonObject("removedCase").getBoolean("isCivil"), is(true));
        assertThat(removedEvent.getJsonObject("removedCase").getBoolean("isGroupMember"), is(false));
        assertThat(removedEvent.getJsonObject("removedCase").getBoolean("isGroupMaster"), is(false));

        if (groupMasterChanged) {
            assertThat(removedEvent.getJsonObject("newGroupMaster").getString("groupId"), is(groupId));
            assertThat(removedEvent.getJsonObject("newGroupMaster").getBoolean("isCivil"), is(true));
            assertThat(removedEvent.getJsonObject("newGroupMaster").getBoolean("isGroupMember"), is(true));
            assertThat(removedEvent.getJsonObject("newGroupMaster").getBoolean("isGroupMaster"), is(true));
        }
    }

    private void verifyInMQForCaseGroupInfoUpdated(final String removedCaseId, final String groupId, final boolean groupMasterChanged) {
        final Optional<JsonObject> message1 = retrieveMessageBody(consumerForCaseGroupInfoUpdated);
        final Optional<JsonObject> message2 = groupMasterChanged ? retrieveMessageBody(consumerForCaseGroupInfoUpdated) : Optional.empty();

        assertTrue(message1.isPresent());
        assertTrue(!groupMasterChanged || message2.isPresent());

        final JsonObject removedEvent = message1.get().getJsonObject("prosecutionCase").getString("id").equals(removedCaseId) ?
                message1.get() : message2.get();

        assertThat(removedEvent.getJsonObject("prosecutionCase").getString("id"), is(removedCaseId));
        assertThat(removedEvent.getJsonObject("prosecutionCase").getString("groupId"), is(groupId));
        assertThat(removedEvent.getJsonObject("prosecutionCase").getBoolean("isGroupMember"), is(false));
        assertThat(removedEvent.getJsonObject("prosecutionCase").getBoolean("isGroupMaster"), is(false));

        if (groupMasterChanged) {
            final JsonObject newGroupMasterEvent = message1.get().getJsonObject("prosecutionCase").getString("id").equals(removedCaseId) ?
                    message2.get() : message1.get();
            assertThat(newGroupMasterEvent.getJsonObject("prosecutionCase").getString("groupId"), is(groupId));
            assertThat(newGroupMasterEvent.getJsonObject("prosecutionCase").getBoolean("isGroupMember"), is(true));
            assertThat(newGroupMasterEvent.getJsonObject("prosecutionCase").getBoolean("isGroupMaster"), is(true));
        }
    }

    private UUID verifyCasesAndGetGroupMasterId(final Set<UUID> caseIds, final List<UUID> removedCaseIds) {
        UUID groupMasterId = null;

        for (final UUID caseId : caseIds) {
            final String payload = pollProsecutionCasesProgressionFor(caseId.toString(), withJsonPath("$.prosecutionCase.id", is(caseId.toString())));
            final JsonObject prosecutionCase = stringToJsonObjectConverter.convert(payload).getJsonObject("prosecutionCase");
            if (removedCaseIds.contains(caseId)) {
                assertThat(prosecutionCase.getBoolean("isGroupMember"), is(false));
                assertThat(prosecutionCase.getBoolean("isGroupMaster"), is(false));
            } else {
                assertThat(prosecutionCase.getBoolean("isGroupMember"), is(true));
                assertThat(prosecutionCase.containsKey("isGroupMaster"), is(true));

                if (prosecutionCase.getBoolean("isGroupMaster")) {
                    if (nonNull(groupMasterId)) {
                        assertThat(format("Only one case can be a group master. {}, {}", groupMasterId, caseId), false);
                    }

                    groupMasterId = caseId;

                }
            }
        }
        assertThat(groupMasterId, is(notNullValue()));
        return groupMasterId;
    }

    private void verifyCaseHearings(final String caseId, final String hearingId) {
        pollForResponse("/prosecutioncases/" + caseId,
                PROGRESSION_QUERY_GET_CASE_HEARINGS,
                randomUUID().toString(),
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].hearingId", is(hearingId))
        );
    }

    private void verifyHearing(final String hearingId, final List<String> groupCaseIds, final String hearingListingStatus) {
        Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases.[*].id", hasItems(groupCaseIds.get(0), groupCaseIds.get(1), groupCaseIds.get(2), groupCaseIds.get(3), groupCaseIds.get(4))),
                withJsonPath("$.hearingListingStatus", is(hearingListingStatus))
        };
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, hearingMatchers);
    }


}