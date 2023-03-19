package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.resteasy.util.HttpResponseCodes.SC_ACCEPTED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingV2ForHmiSlots;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.LaaAPIMServiceStub;
import uk.gov.moj.cpp.progression.util.FileUtil;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;

@SuppressWarnings("squid:S1607")
public class PublicHearingResultedWithFeatureToggleEnabledIT extends AbstractIT {

    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents.createPrivateConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);
    private static final MessageConsumer messageConsumerHearingResultedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.hearing-resulted");
    private static final MessageConsumer messageConsumerProsecutionCasesResultedV2PrivateEvent = privateEvents.createPrivateConsumer("progression.event.prosecution-cases-resulted-v2");
    private static final MessageConsumer messageConsumerProceedingConcludedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.laa-defendant-proceeding-concluded-changed");
    private static final MessageConsumer messageConsumerProceedingConcludedResentPrivateEvent = privateEvents.createPrivateConsumer("progression.event.laa-defendant-proceeding-concluded-resent");
    private static final MessageConsumer messageConsumerNextHearingsRequestedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.next-hearings-requested");
    private static final MessageConsumer messageConsumerDeleteNextHearingsRequestedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.delete-next-hearings-requested");
    private static final MessageConsumer messageConsumerRelatedHearingRequestedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.related-hearing-requested");
    private static final MessageConsumer messageConsumerUnscheduledNextHearingsRequestedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.unscheduled-next-hearings-requested");
    private static final MessageConsumer messageConsumerProsecutionCaseListingNumberUpdatedEvent = privateEvents.createPrivateConsumer("progression.event.prosecution-case-listing-number-updated");
    private static final MessageConsumer messageConsumerProsecutionCaseListingNumberIncreasedEvent = privateEvents.createPrivateConsumer("progression.event.prosecution-case-listing-number-increased");
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");

    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String reportingRestrictionId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
        messageConsumerHearingResultedPrivateEvent.close();
        messageConsumerProsecutionCasesResultedV2PrivateEvent.close();
        messageConsumerNextHearingsRequestedPrivateEvent.close();
        messageConsumerDeleteNextHearingsRequestedPrivateEvent.close();
        messageConsumerRelatedHearingRequestedPrivateEvent.close();
        messageConsumerUnscheduledNextHearingsRequestedPrivateEvent.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageConsumerProsecutionCaseListingNumberUpdatedEvent.close();
        messageConsumerProceedingConcludedPrivateEvent.close();
        messageConsumerProsecutionCaseListingNumberIncreasedEvent.close();
    }

    @Before
    public void setUp() {
        while(privateEvents.retrievePrivateMessageAsString(messageConsumerHearingPopulatedToProbationCaseWorker, 1L).isPresent());
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", true);
        FeatureStubber.stubFeaturesFor(PROGRESSION_CONTEXT, features);
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
        reportingRestrictionId = randomUUID().toString();
    }

    @AfterClass
    public static void teardown() {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", false);
        FeatureStubber.stubFeaturesFor(PROGRESSION_CONTEXT, features);
    }

    @Test
    public void shouldAddJudicialResultToHearingForTheGivenOrderedDate() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForProceedingConcludedPrivateEvent();



        }

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
    @Ignore
    public void shouldNotBeJudicialResultsInNewHearing() throws Exception {

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

        JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }
        verifyInMessagingQueueForCasesReferredToCourts();
        verifyProsecutionCaseListingNumberUpdatedEvent(caseId, 1);
        verifyQueryHearingWithListingNumber(hearingId, 1);
        verifyProsecutionCaseWithListingNumber(0, 1);

        final  String newHearingId;
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-grown.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            JsonPath message = QueueUtil.retrieveMessage(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(Matchers.allOf(
                    withJsonPath("$.hearing.id", not(is(hearingId))))));
            Assert.assertNotNull(message);
            newHearingId = message.getString("hearing.id");

            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }

        final Hearing hearing = getHearingFromQuery();
        final Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        verifyProsecutionCaseWithListingNumber(1, 1);

        while(QueueUtil.retrieveMessageAsString(messageConsumerHearingPopulatedToProbationCaseWorker, 1L).isPresent());
        hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, newHearingId, defendantId, courtCentreId, courtCentreName);
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }
        verifyProsecutionCaseWithListingNumber(1, 2);

        verifyQueryHearingWithListingNumber(newHearingId, 2);
        JsonPath messageDaysMatchers = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)))));
        Assert.assertNull(messageDaysMatchers);

    }

    @Test
    public void shouldAddJudicialResultsToHearingForTheGivenDifferentOrderedDates() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }

        Hearing hearing = getHearingFromQuery();
        Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-30"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }

        hearing = getHearingFromQuery();
        offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(2));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));
        assertThat(offence.getJudicialResults().get(1).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 30)));
    }

    @Test
    public void shouldReplaceJudicialResultInHearingWhenTheResultsSharedForTheSameDay() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }

        Hearing hearing = getHearingFromQuery();
        Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }

        hearing = getHearingFromQuery();
        offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

    }

    @Test
    @Ignore
    public void shouldRaiseNextHearingsRequestedEventWhenNewNextHearingIsAvailableInHearingResults() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForProceedingConcludedPrivateEventNotPresentWhenNotLAA();
            verifyInMessagingQueueForNextHearingsRequestedPrivateEvent();

        }

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
    public void shouldRaiseNextHearingsRequestedEventWhenHmiMultiDay() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-hmi-multi-days.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyPostListCourtHearingV2ForHmiSlots();
        }
    }

    @Test
    @Ignore
    public void shouldDeleteNextHearingWhenNextHearingDeletedInResults() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-next-hearings.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForNextHearingsRequestedPrivateEvent();

        }

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


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-without-new-next-hearings.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForDeleteNextHearingsRequestedPrivateEvent();
        }
    }

    @Test
    @Ignore
    public void shouldRaiseRelatedHearingRequestedEventWhenNewRelatedHearingIsAvailableInHearingResults() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-related-next-hearing.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForRelatedHearingRequestedPrivateEvent();
        }

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

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject(PUBLIC_EVENTS_HEARING_HEARING_RESULTED + "-with-new-unscheduled-next-hearings.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForUnscheduledNextHearingsRequestedPrivateEvent();
        }

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

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
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
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.events.hearing.hearing-resulted-with-defendantjudicialresults-at-defendant-level.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
            verifyInMessagingQueueForProceedingConcludedPrivateEvent();
        }

        final Hearing hearing = getHearingFromQuery();
        final Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

    }

    @Test
    public void whenDefendantProceedingConcludedEventIsTriggeredDuringMultiDayHearing() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-offence1-concluded.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-22"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-offence2-concluded.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-23"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-offence3-concluded.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-24"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-offence4-concluded.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-25"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }
    }

    @Test
    public void whenDefendantProceedingConcludedIsTriggeredAndResultsAreAddedOnCaseAfterCaseIsAdjourned() throws Exception{
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-case-adjourned.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-22"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-defendant-results-added.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-23"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }
    }

    @Test
    public void shouldHaveBothOffencesWhenProceedingConcludedIsTriggeredOffenceByOffence() throws Exception{
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-for-offence2-multioffence.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2022-02-25"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-for-offence1-multioffence.json", caseId,
                            UUID.randomUUID().toString(), defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2022-02-25"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }
    }

    @Test
    public void shouldHaveOffencesWhenProceedingConcludedIsTriggeredOffenceByOffence() throws Exception{
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-for-laa-offence-multioffence.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2022-02-25"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-for-non-laaoffence-multioffence.json", caseId,
                            UUID.randomUUID().toString(), defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2022-02-25"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
        }
    }


    @Test
    public void shouldMakeCaseStatusInactiveWhenResultsAreAddedAtHearingLevel() throws Exception{
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        try (final MessageConsumer messageConsumerHearingResultedCaseUpdatedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.hearing-resulted-case-updated")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-defendant-proceeding-concluded-with-defendant-results-added.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-11-23"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());

            final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingResultedCaseUpdatedPrivateEvent);
            assertTrue(message.isPresent());
            final JsonObject hearingResultedUpdated = message.get();
            final JsonObject prosecutionCase = hearingResultedUpdated.getJsonObject("prosecutionCase");
            assertThat(prosecutionCase.getString("caseStatus"), is("INACTIVE"));
        }
    }


    @Test
    public void whenResultedBeforeLAAGrantAndLAAGrantIsProvidedLaterAndResulted() throws Exception{
        String statusCode = "G2";
        stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
        final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject( "public.events.hearing.hearing-resulted-without-laa.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }

        Hearing hearing = getHearingFromQuery();
        Offence offence = hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        assertThat(offence.getJudicialResults().size(), equalTo(1));
        assertThat(offence.getJudicialResults().get(0).getOrderedDate(), equalTo(LocalDate.of(2021, 03, 29)));

        try (final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
             final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED)) {
            //Record LAA reference
            //When
            recordLAAReference(caseId, defendantId, offenceId, statusCode);

            //Then
            verifyInMessagingQueueForDefendantOffenceUpdated(messageConsumerClientPublicForRecordLAAReference);
            verifyInMessagingQueueForDefendantLegalAidStatusUpdated(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        }

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_EVENTS_HEARING_HEARING_RESULTED, getHearingJsonObject( "public.events.hearing.hearing-resulted-without-laa-adjourned.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId, "2021-03-29"), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingResultedPrivateEvent();
            verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent();
        }
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
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

    private static void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForHearingResultedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingResultedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForProsecutionCasesResultedV2PrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCasesResultedV2PrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForProceedingConcludedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProceedingConcludedPrivateEvent);
        assertTrue(message.isPresent());
        final JsonObject proceedingConcludedEvent = message.get();
        final JsonArray defendantArray = proceedingConcludedEvent.getJsonArray("defendants");
        assertThat(defendantArray.size(), is(1));
        final JsonObject defendantObject = defendantArray.getJsonObject(0);
        assertThat(defendantObject.get("proceedingsConcluded").toString(), is("true"));

        final String requestPayload = FileUtil.getPayload("stub-data/laa-api-proceeding-concluded-request.json")
                .replace("DEFENDANT_ID", defendantObject.getString("id"))
                .replace("HEARING_ID", proceedingConcludedEvent.getString("hearingId"))
                .replace("IS_CONCLUDED",defendantObject.get("proceedingsConcluded").toString() );
        LaaAPIMServiceStub.stubPostLaaAPI(requestPayload);
    }

    private void verifyInMessagingQueueForProceedingConcludedResentPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProceedingConcludedResentPrivateEvent);
        assertTrue(message.isPresent());
        final JsonObject laaDefendantProceedingConcludedChanged = message.get().getJsonObject("laaDefendantProceedingConcludedChanged");
        final String prosecutionCaseId = laaDefendantProceedingConcludedChanged.getJsonString("prosecutionCaseId").getString();
        assertThat(prosecutionCaseId, is(caseId));
        final String hearingId = laaDefendantProceedingConcludedChanged.getJsonString("hearingId").getString();
        assertThat(hearingId, is(hearingId));


    }

    private static void verifyInMessagingQueueForProceedingConcludedPrivateEventNotPresentWhenNotLAA() {
        Awaitility.await()
                .pollDelay(Duration.TEN_SECONDS)
                .pollInterval(Duration.ONE_HUNDRED_MILLISECONDS);
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProceedingConcludedPrivateEvent);
        assertTrue((message.isPresent()));

    }

    private static void verifyInMessagingQueueForNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForNextHearingsRequestedPrivateEvent(final Matcher matchers) {
        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerNextHearingsRequestedPrivateEvent, matchers);
        assertNotNull(message);
    }

    private static void verifyInMessagingQueueForDeleteNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerDeleteNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }


    private static void verifyInMessagingQueueForRelatedHearingRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerRelatedHearingRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForUnscheduledNextHearingsRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerUnscheduledNextHearingsRequestedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated(final MessageConsumer messageConsumerClientPublicForRecordLAAReference) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertThat(message.get().containsKey("addedOffences"), is(false));
        assertThat(message.get().containsKey("deletedOffences"), is(false));
    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated(final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    private void verifyProsecutionCaseListingNumberUpdatedEvent(final String caseId, final int listingNumber){
        JsonPath messageDaysMatchers = QueueUtil.retrieveMessage(messageConsumerProsecutionCaseListingNumberIncreasedEvent, isJson(Matchers.allOf(
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
                withJsonPath("$.prosecutionCase.defendants[0].offences["+offenceIndex+"].listingNumber", is(listingNumber)),
                withoutJsonPath("$.prosecutionCase.defendants[0].offences["+(offenceIndex ==1 ? 0 : 1)+"].listingNumber")
        );
    }

}

