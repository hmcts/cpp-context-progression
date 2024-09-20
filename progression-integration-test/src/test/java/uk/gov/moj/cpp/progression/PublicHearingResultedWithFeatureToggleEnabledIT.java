package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.Duration.ofMillis;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.waitAtMost;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.resteasy.util.HttpResponseCodes.SC_ACCEPTED;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingV2ForHmiSlots;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubCourtApplicationTypes;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.LaaAPIMServiceStub;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private static final JmsMessageConsumerClient messageConsumerProceedingConcludedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.laa-defendant-proceeding-concluded-changed").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerProceedingConcludedResentPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.laa-defendant-proceeding-concluded-resent").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerNextHearingsRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.next-hearings-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerDeleteNextHearingsRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.delete-next-hearings-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerRelatedHearingRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.related-hearing-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerUnscheduledNextHearingsRequestedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.unscheduled-next-hearings-requested").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseListingNumberUpdatedEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-listing-number-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseListingNumberIncreasedEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-listing-number-increased").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();
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
        cleanViewStoreTables();
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        cleanViewStoreTables();
        while (retrieveMessageBody(messageConsumerHearingPopulatedToProbationCaseWorker, 1L).isPresent())
            ;
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

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(Matchers.allOf(
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)))));

        LaaAPIMServiceStub.stubPostLaaAPI();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForProceedingConcludedPrivateEvent(hearingId);

        final Hearing hearing = getHearingFromQuery();
        final Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        // resend defendant proceeding concluded update to LAA
        final String commandPayload = getPayload("progression.command.resend-laa-outcome-concluded.json").replace("CASE_ID", caseId);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/laa/caseOutcome"),
                "application/vnd.progression.command.resend-laa-outcome-concluded+json",
                commandPayload, USER_ID_VALUE_AS_ADMIN.toString());

        assertThat(writeResponse.getStatusCode(), is(SC_ACCEPTED));
        verifyInMessagingQueueForProceedingConcludedResentPrivateEvent();
    }

    @Test
    public void shouldNotBeJudicialResultsInNewHearing() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);
        verifyInMessagingQueueForCasesReferredToCourts();
        verifyProsecutionCaseListingNumberUpdatedEvent(caseId, 1);
        verifyQueryHearingWithListingNumber(hearingId, 1);
        verifyProsecutionCaseWithListingNumber(0, 1);

        final String newHearingId;
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v3").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-grown.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessage(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        final JsonObject eventPayload = jsonEnvelope.get().payloadAsJsonObject();
        final JsonObject listNextHearingsJsonObject = eventPayload.getJsonObject("listNextHearings");
        Assert.assertNotNull(listNextHearingsJsonObject);

        final ListNextHearingsV3 listNextHearings = jsonObjectConverter.convert(listNextHearingsJsonObject, ListNextHearingsV3.class);

        newHearingId = listNextHearings.getHearings().get(0).getId().toString();

        Assert.assertFalse(listNextHearings.getHearings().get(0).getId().equals(listNextHearings.getSeedingHearing().getSeedingHearingId()));


        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();

        final Hearing hearing = getHearingFromQuery();
        final Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        verifyProsecutionCaseWithListingNumber(1, 1);

        while (retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker).isPresent()) ;
        hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, newHearingId, defendantId, courtCentreId, courtCentreName);
        final JmsMessageConsumerClient consumerForCourtDocumentUpdateFailed = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-document-update-failed").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged4 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventConfirmedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged4);
        verifyProsecutionCaseWithListingNumber(1, 2);

        verifyQueryHearingWithListingNumber(newHearingId, 2);
        JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)))));
        Assert.assertNull(messageDaysMatchers);
    }

    @Test
    public void shouldAddJudicialResultsToHearingForTheGivenDifferentOrderedDates() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();

        Hearing hearing = getHearingFromQuery();
        Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged4 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-30"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope2);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged4);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();

        hearing = getHearingFromQuery();
        offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(2));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));
        assertThat(offence.getJudicialResults().get(1).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 30)));
    }

    @Test
    public void shouldReplaceJudicialResultInHearingWhenTheResultsSharedForTheSameDay() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();

        Hearing hearing = getHearingFromQuery();
        Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged4 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope2);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged4);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();

        hearing = getHearingFromQuery();
        offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

    }

    @Test
    public void shouldRaiseNextHearingsRequestedEventWhenNewNextHearingIsAvailableInHearingResults() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForProceedingConcludedPrivateEventNotPresentWhenNotLAA();
        verifyInMessagingQueueForNextHearingsRequestedPrivateEvent();


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

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();


        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForProceedingConcludedPrivateEventNotPresentWhenNotLAA();
        verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();


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

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();
        String applicationId;
        String applicationHearingId;
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        applicationId = verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();
        applicationHearingId = verifyInMessagingQueueForCourtApplicationProceedingInitiatedPrivateEvent();

        final JsonObject applicationHearingConfirmedJson = getApplicationHearingJsonObject("public.listing.hearing-confirmed-applications-only.json", applicationHearingId, applicationId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged4 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), applicationHearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged4);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged5 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged5);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        String newApplicationId = verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();
        verifyInMessagingQueueForDeleteApplicationForCaseRequestedPrivateEvent(applicationId);
        verifyInMessagingQueueForCourtApplicationDeletedPrivateEvent(applicationId);
        verifyInMessagingQueueForCourtApplicationDocumentUpdatedPrivateEvent(applicationId, newApplicationId);
    }

    @Test
    public void shouldTranslateWelshForPostalNotificationWhenApplicationIsAvailableInHearingResultsAndResharedWithWelshRequired() throws Exception {
        stubCourtApplicationTypes("/restResource/referencedata.application-type.json");
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();
        String applicationId;
        String applicationHearingId;
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings-application-welsh.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        applicationId = verifyInMessagingQueueForInitiateApplicationForCaseRequestedPrivateEvent();
        applicationHearingId = verifyInMessagingQueueForCourtApplicationProceedingInitiatedPrivateEvent();
        getHearingForDefendant(applicationHearingId);

        final JsonObject applicationHearingConfirmedJson = getApplicationHearingJsonObject("public.listing.hearing-confirmed-applications-only.json", applicationHearingId, applicationId, courtCentreId, courtCentreName);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), applicationHearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForForWelshTranslationRequired();

    }

    @Test
    public void shouldRaiseNextHearingsRequestedEventWhenHmiMultiDay() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-hmi-multi-days.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyPostListCourtHearingV2ForHmiSlots();
    }

    @Test
    public void shouldDeleteNextHearingWhenNextHearingDeletedInResults() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForNextHearingsRequestedPrivateEvent();


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

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged4 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-without-new-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope2);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged4);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForDeleteNextHearingsRequestedPrivateEvent();
    }

    @Test
    public void shouldRaiseRelatedHearingRequestedEventWhenNewRelatedHearingIsAvailableInHearingResults() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-related-next-hearing.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForRelatedHearingRequestedPrivateEvent();

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

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));


        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(Matchers.allOf(
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)))));

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-unscheduled-next-hearings.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForUnscheduledNextHearingsRequestedPrivateEvent();

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
    public void whenDefendantJudicialResultWithFinalCategoryIsPresentAtDefendantLevel() throws Exception {

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        LaaAPIMServiceStub.stubPostLaaAPI();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);

        verifyInMessagingQueueForCasesReferredToCourts();

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.events.hearing.hearing-resulted-with-defendantjudicialresults-at-defendant-level.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        verifyInMessagingQueueForProceedingConcludedPrivateEvent(hearingId);

        final Hearing hearing = getHearingFromQuery();
        final Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

    }


    @Test
    public void shouldMakeCaseStatusInactiveWhenAllOffencesAreResultedFinal() throws Exception {
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JmsMessageConsumerClient messageConsumerHearingResultedCaseUpdatedPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-resulted-case-updated").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-all-offences-resulted-final.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-23"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingResultedCaseUpdatedPrivateEvent);
        assertTrue(message.isPresent());
        final JsonObject hearingResultedUpdated = message.get();
        final JsonObject prosecutionCase = hearingResultedUpdated.getJsonObject("prosecutionCase");
        assertThat(prosecutionCase.getString("caseStatus"), is("INACTIVE"));
    }


    @Test
    public void whenResultedBeforeLAAGrantAndLAAGrantIsProvidedLaterAndResulted() throws Exception {
        String statusCode = "G2";
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);


        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.events.hearing.hearing-resulted-without-laa.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();

        Hearing hearing = getHearingFromQuery();
        Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForDefendantLegalAidStatusUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).getMessageConsumerClient();
        //Record LAA reference
        //When
        recordLAAReference(caseId, defendantId, offenceId, statusCode);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged3 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventResultedEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject("public.events.hearing.hearing-resulted-without-laa-adjourned.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope2);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged3);
        verifyInMessagingQueueForHearingResultedPrivateEvent();
        verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
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

    private Hearing getHearingFromQuery() {
        final String hearingIdQueryResult = pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON);
        final JsonObject hearingJsonObject = getJsonObject(hearingIdQueryResult);
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

    private void verifyInMessagingQueueForProceedingConcludedPrivateEvent(final String hearingId) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerProceedingConcludedPrivateEvent, isJson(Matchers.allOf(
                withJsonPath("$.hearingId", is(hearingId)))));

        final List<HashMap> defendantArray = message.getJsonObject("defendants");
        assertThat(defendantArray.size(), is(1));
        HashMap map = defendantArray.get(0);
        assertThat(map.get("proceedingsConcluded"), Matchers.is(true));
    }

    private void verifyInMessagingQueueForProceedingConcludedResentPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProceedingConcludedResentPrivateEvent);
        assertTrue(message.isPresent());
        final JsonObject laaDefendantProceedingConcludedChanged = message.get().getJsonObject("laaDefendantProceedingConcludedChanged");
        final String prosecutionCaseId = laaDefendantProceedingConcludedChanged.getJsonString("prosecutionCaseId").getString();
        assertThat(prosecutionCaseId, is(caseId));
        final String hearingId = laaDefendantProceedingConcludedChanged.getJsonString("hearingId").getString();
        assertThat(hearingId, is(hearingId));


    }

    private void verifyInMessagingQueueForProceedingConcludedPrivateEventNotPresentWhenNotLAA() {
        Awaitility.await()
                .pollDelay(Duration.ofSeconds(10))
                .pollInterval(ofMillis(100));
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProceedingConcludedPrivateEvent);
        assertFalse((message.isPresent()));

    }

    private void verifyInMessagingQueueForNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForDeleteNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerDeleteNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }


    private void verifyInMessagingQueueForRelatedHearingRequestedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerRelatedHearingRequestedPrivateEvent);
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

    private void verifyProsecutionCaseListingNumberUpdatedEvent(final String caseId, final int listingNumber) {
        JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerProsecutionCaseListingNumberIncreasedEvent, isJson(Matchers.allOf(
                withJsonPath("$.prosecutionCaseId", CoreMatchers.is(caseId)),
                withJsonPath("$.offenceListingNumbers[0].listingNumber", is(listingNumber)))));
        Assert.assertNotNull(messageDaysMatchers);
    }

    private void verifyQueryHearingWithListingNumber(final String hearingId, final int listingNumber) {
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", Matchers.is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(listingNumber))
        );
    }

    private void verifyProsecutionCaseWithListingNumber(final int offenceIndex, final int listingNumber) {
        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", Matchers.is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", Matchers.is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[" + offenceIndex + "].listingNumber", is(listingNumber)),
                withoutJsonPath("$.prosecutionCase.defendants[0].offences[" + (offenceIndex == 1 ? 0 : 1) + "].listingNumber")
        );
    }

}

