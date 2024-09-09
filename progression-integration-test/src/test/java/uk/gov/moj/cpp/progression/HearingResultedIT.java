package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HearingResultedIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED = "public.hearing.hearing-offence-verdict-updated";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression" +
            ".prosecution-cases-referred-to-court";
    private MessageProducer messageProducerClientPublic;
    private MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated;
    private MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker;
    private MessageConsumer messageConsumerCaseRetentionPolicyRecorded;
    private MessageConsumer messageConsumerCaseRetentionLengthCalculated;

    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
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
    private String reportingRestrictionId;


    @After
    public void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageConsumerCaseRetentionPolicyRecorded.close();
        messageConsumerCaseRetentionLengthCalculated.close();
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    @Before
    public void setUp() {
        messageProducerClientPublic = publicEvents.createPublicProducer();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);
        messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");
        messageConsumerCaseRetentionPolicyRecorded = privateEvents.createPrivateConsumer("progression.event.case-retention-policy-recorded");
        messageConsumerCaseRetentionLengthCalculated = privateEvents.createPrivateConsumer("progression.events.case-retention-length-calculated");
        cleanViewStoreTables();
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        applicationId = randomUUID().toString();
        reportingRestrictionId = randomUUID().toString();
    }


    @Test
    public void shouldUpdateOffenceVerdictWhenRaisedPublicEvent() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();
        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED)
                .withUserId(userId)
                .build();
        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayload(hearingId);
        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, hearingVerdictUpdatedJson, metadata);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict.offenceId", Matchers.is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict.verdictDate", Matchers.is("2017-02-20"))
        );

    }


    @Test
    public void shouldUpdateVerdictOfApplication() throws Exception {
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-applications-only.json",
                        applicationId, hearingId, caseId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForApplicationStatus(applicationId, "LISTED");

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED)
                .withUserId(userId)
                .build();
        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayloadForApplication(hearingId, applicationId);
        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, hearingVerdictUpdatedJson, metadata);


        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].id", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.applicationId", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.verdictDate", Matchers.is("2017-02-20"))
        );

    }

    @Test
    public void shouldUpdateVerdictOfApplicationAndUpdateDefendantForHearing() throws Exception {
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-applications-only.json",
                        applicationId, hearingId, caseId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForApplicationStatus(applicationId, "LISTED");

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED)
                .withUserId(userId)
                .build();
        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayloadForApplication(hearingId, applicationId);
        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, hearingVerdictUpdatedJson, metadata);


        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].id", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.applicationId", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.verdictDate", Matchers.is("2017-02-20"))
        );

        try (final MessageConsumer messageConsumerProsecutionCaseUpdateDefendantsWithMatchedRequestV2 = privateEvents
                .createPrivateConsumer("progression.event.prosecution-case-defendant-updated")) {
            ProsecutionCaseUpdateDefendantWithMatchedHelper prosecutionCaseUpdateDefendantWithMatchedHelper = new ProsecutionCaseUpdateDefendantWithMatchedHelper();
            prosecutionCaseUpdateDefendantWithMatchedHelper.updateDefendant(defendantId, caseId, hearingId);
            doVerifyProsecutionCaseUpdateDefendantsWithMatchedRequestV2(messageConsumerProsecutionCaseUpdateDefendantsWithMatchedRequestV2, defendantId);
        }

    }

    @Test
    public void shouldRaiseHearingPopulatedToProbationCaseWorkerWhenPublicListingHearingConfirmed() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, messageConsumerHearingPopulatedToProbationCaseWorker);
        }
    }


    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private void doVerifyProsecutionCaseUpdateDefendantsWithMatchedRequestV2(final MessageConsumer messageConsumerProsecutionCaseUpdateDefendantsWithMatchedRequestV2, final String defendantId) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseUpdateDefendantsWithMatchedRequestV2);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        assertEquals(prosecutionCaseDefendantListingStatusChanged.getJsonObject("defendant").getString("id"), defendantId); ;
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }


    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    public void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }


    private JsonObject getVerdictPublicEventPayload(final String hearingId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-verdict-updated.json")
                .replaceAll("HEARING_ID", hearingId);
        return stringToJsonObjectConverter.convert(strPayload);
    }
    private JsonObject getVerdictPublicEventPayloadForApplication(final String hearingId, final String applicationId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-verdict-updated-for-application.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }
}

