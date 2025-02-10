package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.waitAtMost;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.resteasy.util.HttpResponseCodes.SC_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.LaaAPIMServiceStub.stubPostLaaAPI;
import static uk.gov.moj.cpp.progression.stub.LaaAPIMServiceStub.verifyLaaProceedingsConcludedCommandInvoked;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingV2ForHmiSlots;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubCourtApplicationTypes;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

@SuppressWarnings("squid:S1607")
public class PublicHearingResultedWithFeatureToggleEnabledIT extends AbstractIT {

    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForWelshTranslationRequired = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.welsh-translation-required").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingResultedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-resulted").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCasesResultedV2PrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-cases-resulted-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerInitiateApplicationForCaseRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.initiate-application-for-case-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerCourtApplicationProceedingInitiatedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-proceedings-initiated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerDeleteApplicationForCaseRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.delete-application-for-case-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerCourtApplicationDeletedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-hearing-deleted").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerCourtApplicationDocumentUpdatePrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-application-document-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerNextHearingsRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.next-hearings-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerDeleteNextHearingsRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.delete-next-hearings-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerUnscheduledNextHearingsRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.unscheduled-next-hearings-requested").getMessageConsumerClient();
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    //compare with  master new consumer added
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String reportingRestrictionId;

    @BeforeEach
    public void setUp() {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", true);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, features);
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        reportingRestrictionId = randomUUID().toString();
    }

    @Test
    public void shouldAddJudicialResultToHearingForTheGivenOrderedDate() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        stubPostLaaAPI();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId));

        // resend defendant proceeding concluded update to LAA
        final String commandPayload = getPayload("progression.command.resend-laa-outcome-concluded.json").replace("CASE_ID", caseId);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/laa/caseOutcome"),
                "application/vnd.progression.command.resend-laa-outcome-concluded+json",
                commandPayload, USER_ID_VALUE_AS_ADMIN.toString());

        assertThat(writeResponse.getStatusCode(), is(SC_ACCEPTED));
        verifyLaaProceedingsConcludedCommandInvoked(2, newArrayList(hearingId, caseId, defendantId));
    }

    @Test
    public void shouldAddJudicialResultsToHearingForTheGivenDifferentOrderedDates() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        verifyHearingWithMatchers(new Matcher[]{
                withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29")),
        });

        final JsonEnvelope publicEventResultedEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-30"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope2);

        verifyHearingWithMatchers(new Matcher[]{
                withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults.length()", is(2)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults[1].orderedDate", is("2021-03-30"))
        });
    }

    @Test
    public void shouldRaiseNextHearingsRequestedEventWhenNewNextHearingIsAvailableInHearingResults() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-10-20")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }

    @Test
    public void shouldNotRaiseNextHearingsRequestedEventWhenApplicationIsAvailableInHearingResults() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-10-20")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }

    @Test
    public void shouldDeleteOldApplicationWhenApplicationIsAvailableInHearingResultsAndReshared() throws Exception {
        stubCourtApplicationTypes("/restResource/referencedata.application-type.json");

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        String applicationId;
        String applicationHearingId;

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        applicationId = verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();
        applicationHearingId = verifyInMessagingQueueForCourtApplicationProceedingInitiatedPrivateEvent();

        final JsonObject applicationHearingConfirmedJson = getApplicationHearingJsonObject("public.listing.hearing-confirmed-applications-only.json", applicationHearingId, applicationId, courtCentreId, courtCentreName);

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), applicationHearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatus(applicationHearingId, "HEARING_INITIALISED");

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        String newApplicationId = verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();
        verifyInMessagingQueueForDeleteApplicationForCaseRequestedPrivateEvent(applicationId);
        verifyInMessagingQueueForCourtApplicationDeletedPrivateEvent(applicationId);
        verifyInMessagingQueueForCourtApplicationDocumentUpdatedPrivateEvent(applicationId, newApplicationId);
    }

    @Test
    public void shouldTranslateWelshForPostalNotificationWhenApplicationIsAvailableInHearingResultsAndResharedWithWelshRequired() throws Exception {
        stubCourtApplicationTypes("/restResource/referencedata.application-type.json");
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application-welsh.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        String applicationId = verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();
        String applicationHearingId = verifyInMessagingQueueForCourtApplicationProceedingInitiatedPrivateEvent();
        getHearingForDefendant(applicationHearingId);

        final JsonObject applicationHearingConfirmedJson = getApplicationHearingJsonObject("public.listing.hearing-confirmed-applications-only.json", applicationHearingId, applicationId, courtCentreId, courtCentreName);

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), applicationHearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(applicationHearingId, "HEARING_INITIALISED");

        verifyInMessagingQueueForForWelshTranslationRequired();

    }

    @Test
    public void shouldRaiseNextHearingsRequestedEventWhenHmiMultiDay() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-hmi-multi-days.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");
        verifyPostListCourtHearingV2ForHmiSlots();
    }

    @Test
    public void shouldDeleteNextHearingWhenNextHearingDeletedInResults() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-10-20")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

        final JsonEnvelope publicEventResultedEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-without-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope2);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");
        verifyInMessagingQueueForDeleteNextHearingsRequestedPrivateEvent();
    }

    @Test
    public void shouldMoveNewDefendantToNewNextHearingWhenHearingAmended() throws Exception {
        JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        System.out.println("hearingId : " + hearingId);

        final String defendantId2 = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId2));

        final String newHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        System.out.println("newHearingId : " + newHearingId);
        final JmsMessageConsumerClient messageConsumerHearingResultedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.offences-moved-to-new-next-hearing").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPublicEvent1 = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendants-added-to-court-proceedings").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPublicEvent2 = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendants-added-to-case").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPublicEvent3 = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendants-added-to-hearing").getMessageConsumerClient();


        final JsonObject publicEvent = Json.createObjectBuilder().add("newHearingId", newHearingId).add("seedingHearingId", randomUUID().toString())
                .add("oldHearingIds", Json.createArrayBuilder().add(hearingId)).build();
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata("public.listing.offences-moved-to-next-hearing", userId), publicEvent);
        messageProducerClientPublic.sendMessage("public.listing.offences-moved-to-next-hearing", publicEventEnvelope);

        assertTrue(retrieveMessageBody(messageConsumerHearingResultedPrivateEvent).isPresent());
        assertTrue(retrieveMessageBody(messageConsumerPublicEvent1).isPresent());
        assertTrue(retrieveMessageBody(messageConsumerPublicEvent2).isPresent());
        assertTrue(retrieveMessageBody(messageConsumerPublicEvent3).isPresent());


        Hearing newHearing = getHearingFromQuery(newHearingId);
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().size(), is(2));

    }

    @Test
    public void shouldMoveNewOffenceToNewNextHearingWhenHearingAmended() throws Exception {
        final String newOffenceId = randomUUID().toString();
        JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChangedV2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChangedV2);
        System.out.println("hearingId : " + hearingId);

        JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);


        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChangedV2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v3").getMessageConsumerClient();

        JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        String nextHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForNextHearingsRequestedPrivateEvent();
        System.out.println("nextHearing : " + nextHearingId);
        hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, nextHearingId, defendantId, courtCentreId, courtCentreName);


        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChangedV2);

        verifyInMessagingQueueForCasesReferredToCourts();

        Hearing newHearing = getHearingFromQuery(nextHearingId);
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(1));

        final ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, randomUUID().toString());;
        helper.updateOffences(newOffenceId, "TFL123");
        JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-offences-updated").getMessageConsumerClient();
        helper.verifyInActiveMQ(privateEventsConsumer);

         newHearing = getHearingFromQuery(nextHearingId);
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(2));

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        String newHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChangedV2);
        if(newHearingId.equals(hearingId) || newHearingId.equals(nextHearingId)){
            newHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChangedV2);
        }
        System.out.println("newHearingId : " + newHearingId);
        hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, newHearingId, defendantId, courtCentreId, courtCentreName);


        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChangedV2);

        newHearing = getHearingFromQuery(newHearingId);
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(1));

        final JmsMessageConsumerClient messageConsumerOffencesMovedToNewNextHearingPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.offences-moved-to-new-next-hearing").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPublicEvent1 = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-offences-changed").getMessageConsumerClient();

        final JsonObject publicEvent = Json.createObjectBuilder().add("newHearingId", newHearingId).add("seedingHearingId", hearingId)
                .add("oldHearingIds", Json.createArrayBuilder().add(nextHearingId)).build();
        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata("public.listing.offences-moved-to-next-hearing", userId), publicEvent);
        messageProducerClientPublic.sendMessage("public.listing.offences-moved-to-next-hearing", publicEventEnvelope);

        assertTrue(retrieveMessageBody(messageConsumerOffencesMovedToNewNextHearingPrivateEvent).isPresent());
        assertTrue(retrieveMessageBody(messageConsumerPublicEvent1).isPresent());

        newHearing = getHearingFromQuery(newHearingId);
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(newHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(2));


    }


    @Test
    public void shouldRaiseRelatedHearingRequestedEventWhenNewRelatedHearingIsAvailableInHearingResults() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-related-next-hearing.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-10-20")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }

    @Test
    public void shouldRaiseUnscheduledNexHearingsRequestedEventWhenNewUnscheduledNextHearingIsAvailableInHearingResults() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-unscheduled-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        verifyInMessagingQueueForUnscheduledNextHearingsRequestedPrivateEvent();
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-10-20")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }

    @Disabled("SNI-6520 is disabled this test, this test is wrong")
    @Test
    public void shouldSendLAAConcludedEventWithOffencesWhenConsecutiveHearingResultedForSingleOffenceWithNoJudiciaryResults() throws Exception {
        final String offenceId1 = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        final String offenceId2 = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        Consumer<String> resultHearingWithJudiciaryResult = (offenceId) -> {
            final JsonObject hearingConfirmedJson = getHearingJsonObject("public.events.hearing.hearing-resulted-with-one-offence-without-judical-results.json", caseId, hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29", s -> s.replace("OFFENCE_ID1", offenceId));

            final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), hearingConfirmedJson);

            messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
            pollHearingWithStatus(hearingId, "HEARING_RESULTED");
            verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId, offenceId));

        };
        resultHearingWithJudiciaryResult.accept(offenceId1);
        resultHearingWithJudiciaryResult.accept(offenceId2);
    }

    @Test
    public void whenDefendantJudicialResultWithFinalCategoryIsPresentAtDefendantLevel() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        stubPostLaaAPI();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.events.hearing.hearing-resulted-with-defendantjudicialresults-at-defendant-level.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        verifyHearingWithMatchers(new Matcher[]{
                withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29")),
        });
    }


    @Test
    public void shouldMakeCaseStatusInactiveWhenAllOffencesAreResultedFinal() throws Exception {
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-all-offences-resulted-final.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-23"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.caseStatus", is("INACTIVE")));
    }


    @Test
    public void whenResultedBeforeLAAGrantAndLAAGrantIsProvidedLaterAndResulted() throws Exception {
        String statusCode = "G2";
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.events.hearing.hearing-resulted-without-laa.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        verifyHearingWithMatchers(new Matcher[]{
                withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults[0].orderedDate", is("2021-03-29")),
        });

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();
        //Record LAA reference
        //When
        recordLAAReference(caseId, defendantId, offenceId, statusCode);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {

        final AtomicReference<String> hearingId = new AtomicReference<>();

        waitAtMost(Duration.ofMinutes(1)).until(() -> {
                    final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
                    final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();

                    if (prosecutionCaseDefendantListingStatusChanged.containsKey("hearing") && prosecutionCaseDefendantListingStatusChanged.toString().contains(caseId)) {
                        hearingId.set(prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id"));
                        return true;
                    } else {
                        return false;
                    }
                }
        );

        return hearingId.get();
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged, final Matcher matcher) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerProsecutionCaseDefendantListingStatusChanged, matcher);
        return message.getJsonObject("hearing.id");
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

    private JsonObject getApplicationHearingJsonObject(final String path, final String hearingId,
                                                       final String applicationId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName,
                                            final String reportingRestrictionId, final String orderedDate, Function<String, String> payloadModifier) {
        final String payload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("ORDERED_DATE", orderedDate)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);
        String modifiedPayload = payloadModifier.apply(payload);

        return stringToJsonObjectConverter.convert(modifiedPayload);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName,
                                            final String reportingRestrictionId, final String orderedDate) {
        final String payload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("ORDERED_DATE", orderedDate)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);

        return stringToJsonObjectConverter.convert(payload);
    }

    private Hearing verifyHearingWithMatchers(final Matcher<? super ReadContext>[] matchers) {
        final String hearingPayloadAsString = getHearingForDefendant(hearingId, matchers);
        final JsonObject hearingJsonObject = getJsonObject(hearingPayloadAsString);
        JsonObject hearingJson = hearingJsonObject.getJsonObject("hearing");
        return jsonObjectConverter.convert(hearingJson, Hearing.class);
    }

    private void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForForWelshTranslationRequired() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForWelshTranslationRequired);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForHearingResultedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingResultedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCasesResultedV2PrivateEvent);
        assertTrue(message.isPresent());
    }

    private String verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerInitiateApplicationForCaseRequestedPrivateEvent);
        assertTrue(message.isPresent());
        return message.get().getString("applicationId");
    }

    private String verifyInMessagingQueueForCourtApplicationProceedingInitiatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerCourtApplicationProceedingInitiatedPrivateEvent);
        assertTrue(message.isPresent());
        return message.get().getJsonObject("courtHearing").getString("id");
    }

    private void verifyInMessagingQueueForDeleteApplicationForCaseRequestedPrivateEvent(final String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerDeleteApplicationForCaseRequestedPrivateEvent);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("applicationId"), is(applicationId));

    }

    private void verifyInMessagingQueueForCourtApplicationDeletedPrivateEvent(final String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerCourtApplicationDeletedPrivateEvent);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("applicationId"), is(applicationId));

    }

    private void verifyInMessagingQueueForCourtApplicationDocumentUpdatedPrivateEvent(final String oldApplicationId, final String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerCourtApplicationDocumentUpdatePrivateEvent);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("applicationId"), is(applicationId));
        assertThat(message.get().getString("oldApplicationId"), is(oldApplicationId));

    }

    private void verifyInMessagingQueueForNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForDeleteNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerDeleteNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForUnscheduledNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerUnscheduledNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForDefendantOffenceUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertThat(message.get().containsKey("addedOffences"), is(false));
        assertThat(message.get().containsKey("deletedOffences"), is(false));
    }

    private void verifyInMessagingQueueForDefendantLegalAidStatusUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    private Hearing getHearingFromQuery(final String hearingId) {
        final String hearingIdQueryResult = pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON);
        final JsonObject hearingJsonObject = getJsonObject(hearingIdQueryResult);
        JsonObject hearingJson = hearingJsonObject.getJsonObject("hearing");
        return jsonObjectConverter.convert(hearingJson, Hearing.class);
    }
}

