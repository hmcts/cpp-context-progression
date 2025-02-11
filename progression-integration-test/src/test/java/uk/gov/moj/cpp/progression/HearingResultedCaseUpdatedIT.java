package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForGroupCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendPublicEvent;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingResultedCaseUpdatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED_MEMBER_CASE = "public.hearing.resulted-member-cases";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED_V2 = "public.events.hearing.hearing-resulted-case-updated";
    private static final String PUBLIC_HEARING_RESULTED_MULTIPLE_PROSECUTION_CASE_V2 = "public.events.hearing.hearing-resulted-multiple-prosecution-cases";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED = "public.progression.hearing-resulted";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final JmsMessageConsumerClient messageConsumerClientPublicForHearingResultedCaseUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED).getMessageConsumerClient();
    private final JmsMessageConsumerClient messageConsumerClientPublicForHearingResulted = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_HEARING_RESULTED).getMessageConsumerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String offenceId;
    private String groupId;
    private String newCourtCentreId;
    private String bailStatusCode;
    private String bailStatusDescription;
    private String bailStatusId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String courtCentreName;


    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = randomUUID().toString();
        groupId = randomUUID().toString();
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Croydon Magistrate's Court";
        bailStatusCode = "C";
        bailStatusDescription = "Remanded into Custody";
        bailStatusId = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
    }

    @Test
    public void shouldUpdateHearingResultedCaseUpdatedEventWhenMultipleProsecutionMemberCasesArePresent() throws Exception {
        final int caseCount = 2;
        final Map<UUID, Pair<UUID, UUID>> caseDefendantOffence = Stream.generate(UUID::randomUUID).limit(caseCount)
                .collect(Collectors.toMap(caseId -> caseId, caseId -> new Pair<>(UUID.fromString(defendantId), UUID.fromString(offenceId))));

        final UUID masterCaseId = caseDefendantOffence.keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No case found!"));
        final UUID caseId2 = caseDefendantOffence.keySet().stream().filter(c -> !c.toString().equals(masterCaseId.toString())).findFirst().orElseThrow(() -> new RuntimeException("No case found!"));
        initiateCourtProceedingsForGroupCases(masterCaseId, caseDefendantOffence, listedStartDateTime, earliestStartDateTime, groupId, newCourtCentreId, courtCentreName);
        hearingId = pollCaseAndGetHearingForDefendant(masterCaseId.toString(), defendantId);
        sendPublicEvent(
                PUBLIC_HEARING_RESULTED, getHearingWithMultipleCasesJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED_MEMBER_CASE + ".json", masterCaseId.toString(), caseId2.toString(),
                        hearingId, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId, groupId, offenceId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(masterCaseId.toString(), getMemberCaseUpdatedMatchers(masterCaseId.toString()));
    }

    @Test
    public void shouldUpdateHearingResultedCaseUpdatedV2() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED_V2 + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        pollProsecutionCasesProgressionFor(caseId, getDefendantUpdatedMatchers());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
        verifyInMessagingQueueForHearingResulted();
    }

    @Test
    public void shouldRaiseUpdateHearingCaseUpdatedEventWhenMultipleProsecutionCasesArePresentV2() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String caseId2 = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId2, defendantId);

        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithMultipleCasesJsonObject(PUBLIC_HEARING_RESULTED_MULTIPLE_PROSECUTION_CASE_V2 + ".json", caseId, caseId2,
                hearingId, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId, groupId, offenceId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        pollProsecutionCasesProgressionFor(caseId, getDefendantUpdatedMatchers());
    }

    private Matcher[] getDefendantUpdatedMatchers() {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.code", equalTo(bailStatusCode)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description", equalTo(bailStatusDescription)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.id", equalTo(bailStatusId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(INACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", equalTo(true))
        };
    }

    private Matcher[] getMemberCaseUpdatedMatchers(final String masterCaseId) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(masterCaseId)),
                withJsonPath("$.prosecutionCase.defendants[0].defendantCaseJudicialResults[0].label", equalTo("defendants")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].label", equalTo("judicialResults-offence"))
        };
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private JsonObject getHearingWithMultipleCasesJsonObject(final String path, final String caseId1, final String caseId2, final String hearingId,
                                                             final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                             final String bailStatusDescription, final String bailStatusId, final String groupId,
                                                             final String offenceId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID_1", caseId1)
                        .replaceAll("CASE_ID_2", caseId2)
                        .replaceAll("RANDOM_OFFENCE_ID", offenceId)
                        .replaceAll("RANDOM_GROUP_ID", groupId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private void verifyInMessagingQueueForHearingResultedCaseUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingResultedCaseUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("prosecutionCase").getString("caseStatus"), equalTo("INACTIVE"));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getBoolean("proceedingsConcluded"), equalTo(true));
    }

    private void verifyInMessagingQueueForHearingResulted() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingResulted);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("caseStatus"), equalTo("INACTIVE"));
    }



}

