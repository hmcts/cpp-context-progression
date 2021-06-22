package uk.gov.moj.cpp.progression;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.util.Utilities;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FeatureToggleUtil.enableAmendReshareFeature;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.Utilities.listenForPrivateEvent;

public class HearingResultedUnscheduledListingIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING = "public.hearing.resulted-unscheduled-listing";
    private static final String PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 = "public.events.hearing.hearing-resulted-unscheduled-listing";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED = "progression.event" +
            ".prosecutionCase-defendant-listing-status-changed";
    private static final String PROGRESSION_EVENT_UNSCHEDULED_HEARING_RECORDED = "progression.event.unscheduled-hearing-recorded";

    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression" +
            ".prosecution-cases-referred-to-court";

    private static final String PROGRESSION_EVENT_UNSCHEDULED_HEARING_ALLOCATION_NOTIFIED = "progression.event.unscheduled-hearing-allocation-notified";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);

    public static final String EXPECTED_OFFENCE_ID = "333bdd2a-6b7a-4002-bc8c-5c6f93844f41";
    public static final String EXPECTED_JUDICIAL_RESULT_ID = "94d6e18a-4114-11ea-b77f-2e728ce88125";
    public static final String EXPECTED_JUDICIAL_RESULT_TYPEID = "ed34136f-2a13-45a4-8d4f-27075ae3a8a9";
    public static final String EXPECTED_JUDICIAL_RESULT_PROMPT_TYPEID = "e5d8792c-4114-11ea-b77f-2e728ce88125";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
    }

    @Before
    public void setUp() {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        ListingStub.stubListCourtHearing();

        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
    }

    @Test
    public void shouldListUnscheduledHearingsWithWCPNAndRaiseNotificationEvent() throws Exception {
        enableAmendReshareFeature(false);

        final String existingHearingId = prepareHearingForTest();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(PROGRESSION_EVENT_UNSCHEDULED_HEARING_RECORDED)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, existingHearingId);
        final String unscheduledHearingId =  defendantListingStatusChangedPayload.getString("hearing.id");
        assertThat(defendantListingStatusChangedPayload.getString("notifyNCES").toLowerCase(), is("true"));
        eventListenerForDefendantListinStatusChanged.close();

        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event
        final MessageConsumer messageConsumer = privateEvents
                .createConsumer(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        doVerifyEventIsNotRaised(messageConsumer, existingHearingId, unscheduledHearingId);

        //Allocate Hearing to see Notification event is raised.
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, unscheduledHearingId, defendantId, courtCentreId, courtCentreName);

        Utilities.EventListener eventListenerForNotificationEvent = listenForPrivateEvent(PROGRESSION_EVENT_UNSCHEDULED_HEARING_ALLOCATION_NOTIFIED)
                .withFilter(isJson(withJsonPath("$.hearing.id", is(unscheduledHearingId))));

        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);
        eventListenerForNotificationEvent.waitFor();
        eventListenerForNotificationEvent.close();
    }

    @SuppressWarnings("squid:S1607")
    @Ignore
    @Test
    public void shouldListUnscheduledHearingsV2() throws Exception {
        enableAmendReshareFeature(true);

        final String existingHearingId = prepareHearingForTest();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(PROGRESSION_EVENT_UNSCHEDULED_HEARING_RECORDED)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, existingHearingId);
        final String unscheduledHearingId =  defendantListingStatusChangedPayload.getString("hearing.id");


        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event
        final MessageConsumer messageConsumer = privateEvents
                .createConsumer(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        final JsonPath defendantListingStatusChangedPayload2 = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload2, existingHearingId);
        final String unscheduledHearingIdNew =  defendantListingStatusChangedPayload2.getString("hearing.id");
        eventListenerForDefendantListinStatusChanged.close();

        doVerifyEventIsNotRaised(messageConsumer, existingHearingId, unscheduledHearingIdNew);


    }

    @Test
    public void shouldKeepsCpsOrganisationAndListUnscheduledHearings() throws Exception {
        enableAmendReshareFeature(false);

        final String existingHearingId = prepareHearingForTestWithInitiate();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(PROGRESSION_EVENT_UNSCHEDULED_HEARING_RECORDED)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, existingHearingId);
        final String unscheduledHearingId =  defendantListingStatusChangedPayload.getString("hearing.id");

        eventListenerForDefendantListinStatusChanged.close();

        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event
        final MessageConsumer messageConsumer = privateEvents
                .createConsumer(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        doVerifyEventIsNotRaised(messageConsumer, existingHearingId, unscheduledHearingId);

        pollProsecutionCasesProgressionFor(caseId, getMatcherForCpsOrganisation());

    }

    @Test
    public void shouldKeepsCpsOrganisationAndListUnscheduledHearingsV2() throws Exception {
        enableAmendReshareFeature(true);

        final String existingHearingId = prepareHearingForTestWithInitiate();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(PROGRESSION_EVENT_UNSCHEDULED_HEARING_RECORDED)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, existingHearingId);
        final String unscheduledHearingId =  defendantListingStatusChangedPayload.getString("hearing.id");

        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event
        final MessageConsumer messageConsumer = privateEvents
                .createConsumer(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                        existingHearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        final JsonPath defendantListingStatusChangedPayload2 = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload2, existingHearingId);
        final String unscheduledHearingIdNew =  defendantListingStatusChangedPayload2.getString("hearing.id");
        eventListenerForDefendantListinStatusChanged.close();

        doVerifyEventIsNotRaised(messageConsumer, existingHearingId, unscheduledHearingIdNew);

        pollProsecutionCasesProgressionFor(caseId, getMatcherForCpsOrganisation());

    }

    private Matcher[] getMatcherForCpsOrganisation() {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01"))
        };
    }

    private void doVerifyDefendantListingStatusChangedPayload(final JsonPath defendantListingStatusChangedPayload, final String existingHearingId) {
        final String unscheduledHearingId =  defendantListingStatusChangedPayload.getString("hearing.id");
        assertThat(unscheduledHearingId, is(not(nullValue())));

        final List<HashMap> prosecutionCases = defendantListingStatusChangedPayload.getJsonObject("hearing.prosecutionCases");
        assertThat(prosecutionCases.size(), is(1));
        HashMap map = prosecutionCases.get(0);
        assertThat(map.get("id"), is(caseId));

        final List<HashMap> defendants = defendantListingStatusChangedPayload.getJsonObject("hearing.prosecutionCases[0].defendants");
        assertThat(defendants.size(), is(1));
        assertThat(defendants.get(0).get("id"), is(defendantId));

        final List<HashMap> offences = defendantListingStatusChangedPayload.getJsonObject("hearing.prosecutionCases[0].defendants[0].offences");
        assertThat(offences.size(), is(1));
        assertThat(offences.get(0).get("id"), is(EXPECTED_OFFENCE_ID));

        final List<HashMap> judicialResults = defendantListingStatusChangedPayload.getJsonObject("hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults");
        assertThat(judicialResults.size(), is(1));
        assertThat(judicialResults.get(0).get("judicialResultId"), is(EXPECTED_JUDICIAL_RESULT_ID));
        assertThat(judicialResults.get(0).get("judicialResultTypeId"), is(EXPECTED_JUDICIAL_RESULT_TYPEID));

        final List<HashMap> judicialResultPrompts = defendantListingStatusChangedPayload.getJsonObject("hearing.prosecutionCases[0].defendants[0]" +
                ".offences[0].judicialResults[0].judicialResultPrompts");
        assertThat(judicialResultPrompts.size(), is(1));
        assertThat(judicialResultPrompts.get(0).get("judicialResultPromptTypeId"), is(EXPECTED_JUDICIAL_RESULT_PROMPT_TYPEID));
    }

    private void doVerifyRecordedEventPayload(final JsonPath recordedEventPayload, final String existingHearingId, final String unscheduledHearingId) {
        assertThat(recordedEventPayload.getString("hearingId"), is(existingHearingId));
        List<String> unscheduledHearingIds = recordedEventPayload.getJsonObject("unscheduledHearingIds");
        assertThat(unscheduledHearingIds.size(), is(1));
        assertThat(unscheduledHearingIds.get(0), is(unscheduledHearingId));
    }

    private String prepareHearingForTest() throws Exception{
        final MessageConsumer messageConsumer = privateEvents
                .createConsumer(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        String hearingIdInResponse = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumer);
        messageConsumer.close();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingIdInResponse, defendantId, courtCentreId, courtCentreName);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        verifyInMessagingQueueForCasesReferredToCourts();
        return hearingIdInResponse;
    }

    private String prepareHearingForTestWithInitiate() throws Exception{
        final MessageConsumer messageConsumer = privateEvents
                .createConsumer(PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED);

        //addProsecutionCaseToCrownCourt(caseId, defendantId);
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        String hearingIdInResponse = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumer);
        messageConsumer.close();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingIdInResponse, defendantId, courtCentreId, courtCentreName);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        verifyInMessagingQueueForCasesReferredToCourts();
        return hearingIdInResponse;
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumer){
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumer);
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

    private static void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private static void doVerifyEventIsNotRaised(final MessageConsumer messageConsumer, final String originalHearingId, final String hearingId) {

        Optional<JsonPath> message;
        do {
            message = QueueUtil.retrieveMessage(messageConsumer, 1000L);
        } while (message.isPresent() && hearingIdIsOneOf(message.get(), originalHearingId, hearingId));

        assertFalse(message.isPresent());
    }

    private static boolean hearingIdIsOneOf(JsonPath jsonPath, String h1, String h2){
        final String s = jsonPath.getString("hearing.id");
        return h1.equals(s) || h2.equals(s);
    }

}
