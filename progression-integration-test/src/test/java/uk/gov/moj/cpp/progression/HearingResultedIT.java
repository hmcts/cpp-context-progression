package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.json.JSONException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetLatestHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;

public class HearingResultedIT extends AbstractIT {
    private static final String PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED = "public.hearing.hearing-offence-verdict-updated";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression" +
            ".prosecution-cases-referred-to-court";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT).getMessageConsumerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String applicationId;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        applicationId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateOffenceVerdictAndNotifyProbationWhenHearingConfirmedAndOffenceVerdictUpdated() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);
        verifyProbationHearingCommandInvoked(newArrayList(hearingId));

        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayload(hearingId);
        final JsonEnvelope publicEventUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, userId), hearingVerdictUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, publicEventUpdatedEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict.offenceId", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict.verdictDate", is("2017-02-20"))
        );
    }

    @Test
    public void shouldAddTheCaseToUnAllocatedHearingWhenHearingExtendedToUnAllocatedHearing() throws IOException {

        String hearingId2;
        String prosecutionCaseId2 = randomUUID().toString();
        String defendantId2 = randomUUID().toString();

        hearingId = createHearing(caseId, defendantId);
        hearingId2 = createHearing(prosecutionCaseId2, defendantId2);

        final String hearingId3;
        final JsonObject payload = getHearingJsonObject("public.events.hearing.hearing-resulted-unscheduled-listing.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId), payload);
        messageProducerClientPublic.sendMessage("public.events.hearing.hearing-resulted", publicEventEnvelope);

        hearingId3 = pollCaseAndGetLatestHearingForDefendant(caseId, defendantId, 2, List.of(hearingId));

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId3, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelopeForConfirm = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelopeForConfirm);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JsonObject payload2 = getHearingJsonObjectWithExistingHearingId("public.events.hearing.hearing-resulted-with-existing-next-hearing.json", prosecutionCaseId2,
                hearingId2, defendantId2, newCourtCentreId, newCourtCentreName, hearingId3);
        final JsonEnvelope publicEventEnvelope2 = envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId), payload2);
        messageProducerClientPublic.sendMessage("public.events.hearing.hearing-resulted", publicEventEnvelope2);

        pollForHearing(hearingId3,
                withJsonPath("$.hearing.id", is(hearingId3)),
                withJsonPath("$.hearing.prosecutionCases.length()", is(2)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].seedingHearing", is(notNullValue())),
                withJsonPath("$.hearing.prosecutionCases[1].defendants[0].offences[0].seedingHearing", is(notNullValue()))
        );
    }

    @Test
    public void shouldAddTheNewOffenceToUnAllocatedHearingWhenOffenceAddedToCase() throws JSONException, IOException {
        hearingId = createHearing(caseId, defendantId);

        final JsonObject payload = getHearingJsonObject("public.events.hearing.hearing-resulted-unscheduled-listing.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId), payload);
        messageProducerClientPublic.sendMessage("public.events.hearing.hearing-resulted", publicEventEnvelope);

        final String hearingId2 = pollCaseAndGetLatestHearingForDefendant(caseId, defendantId, 2, List.of(hearingId));

        pollForHearing(hearingId2,
                withJsonPath("$.hearing.id", is(hearingId2)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences.length()", is(1))
        );

        final ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, "3789ab16-0bb7-4ef1-87ef-c936bf0364f1");
        helper.updateOffences(randomUUID().toString(), OFFENCE_CODE);
        helper.verifyInMessagingQueueForOffencesUpdated(newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-offences-changed").getMessageConsumerClient());

        pollForHearing(hearingId2,
                withJsonPath("$.hearing.id", is(hearingId2)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences.length()", is(2))
        );
    }

    @Test
    public void shouldUpdateVerdictOfApplication() throws Exception {
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-applications-only.json",
                applicationId, hearingId, caseId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollForApplicationStatus(applicationId, "LISTED");

        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayloadForApplication(hearingId, applicationId);
        final JsonEnvelope publicEventUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, userId), hearingVerdictUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, publicEventUpdatedEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].id", is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.applicationId", is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.verdictDate", is("2017-02-20"))
        );

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return getHearingJsonObjectWithExistingHearingId(path, caseId, hearingId, defendantId, courtCentreId, courtCentreName, "");
    }

    private JsonObject getHearingJsonObjectWithExistingHearingId(final String path, final String caseId, final String hearingId,
                                                                 final String defendantId, final String courtCentreId, final String courtCentreName, final String existingHearingId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("EXISTING_H_ID", existingHearingId)
        );
    }


    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    public void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private JsonObject getVerdictPublicEventPayload(final String hearingId) {
        final String strPayload = getPayload("public.hearing.hearing-offence-verdict-updated.json")
                .replaceAll("HEARING_ID", hearingId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getVerdictPublicEventPayloadForApplication(final String hearingId, final String applicationId) {
        final String strPayload = getPayload("public.hearing.hearing-offence-verdict-updated-for-application.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private String createHearing(final String caseId, final String defendantId) throws IOException {
        final String hearingId;
        final String materialIdActive = randomUUID().toString();
        final String materialIdDeleted = randomUUID().toString();
        final String referralReasonId = randomUUID().toString();
        final String listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        final String defendantDOB = PAST_LOCAL_DATE.next().toString();

        initiateCourtProceedingsForDefendantMatching(caseId, defendantId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForCasesReferredToCourts();

        return hearingId;
    }
}

