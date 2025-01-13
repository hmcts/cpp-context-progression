package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyListUnscheduledHearingRequestsAsStreamV2;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.Utilities.listenForPrivateEvent;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingResultedUnscheduledListingIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 = "public.events.hearing.hearing-resulted-unscheduled-listing";
    private static final String PUBLIC_HEARING_RESULTED_WITH_APPLICATION_RESULT_UNSCHEDULED_LISTING_V2 = "public.events.hearing.hearing-resulted-unscheduled-listing-with-application-resulted";

    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final JmsMessageConsumerClient consumerForDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForUnscheduledHearingRecorded = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.unscheduled-hearing-recorded").getMessageConsumerClient();

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-cases-referred-to-court").getMessageConsumerClient();
    public static final String EXPECTED_OFFENCE_ID = "333bdd2a-6b7a-4002-bc8c-5c6f93844f41";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;

    @BeforeAll
    public static void setUpClass() {
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        ListingStub.stubListCourtHearing();
    }

    @BeforeEach
    public void setUp() {

        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
    }


    @SuppressWarnings("squid:S1607")
    @Test
    public void shouldListUnscheduledHearingsV2() throws Exception {
        final String existingHearingId = prepareHearingForTest();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(consumerForDefendantListingStatusChanged)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(consumerForUnscheduledHearingRecorded)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, EXPECTED_OFFENCE_ID);
        final String unscheduledHearingId = defendantListingStatusChangedPayload.getString("hearing.id");


        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event

        final JsonEnvelope publicEventEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope2);

        final JsonPath defendantListingStatusChangedPayload2 = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload2, EXPECTED_OFFENCE_ID);

        verifyListUnscheduledHearingRequestsAsStreamV2(unscheduledHearingId, "1 week");
    }

    @Test
    public void shouldKeepsCpsOrganisationAndListUnscheduledHearingsV2() throws Exception {
        final String existingHearingId = prepareHearingForTestWithInitiate();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(consumerForDefendantListingStatusChanged)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(consumerForUnscheduledHearingRecorded)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, EXPECTED_OFFENCE_ID);
        final String unscheduledHearingId = defendantListingStatusChangedPayload.getString("hearing.id");

        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event

        final JsonEnvelope publicEventEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope2);

        final JsonPath defendantListingStatusChangedPayload2 = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload2, EXPECTED_OFFENCE_ID);

        pollProsecutionCasesProgressionFor(caseId, getMatcherForCpsOrganisation());

        verifyListUnscheduledHearingRequestsAsStreamV2(unscheduledHearingId, "1 week");
    }

    @SuppressWarnings("squid:S1607")
    @Test
    public void shouldListUnscheduledHearingsV2WhenApplicationResultedWithCase() throws Exception {
        final String existingHearingId = prepareHearingForTest();
        Utilities.EventListener eventListenerForDefendantListinStatusChanged = listenForPrivateEvent(consumerForDefendantListingStatusChanged)
                .withFilter(isJson(withJsonPath("$.hearing.id", not(existingHearingId))));

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(consumerForUnscheduledHearingRecorded)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject( PUBLIC_HEARING_RESULTED_WITH_APPLICATION_RESULT_UNSCHEDULED_LISTING_V2+ ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final JsonPath defendantListingStatusChangedPayload = eventListenerForDefendantListinStatusChanged.waitFor();
        doVerifyDefendantListingStatusChangedPayload(defendantListingStatusChangedPayload, "3789ab16-0bb7-4ef1-87ef-c936bf0364f1");
        final String unscheduledHearingId = defendantListingStatusChangedPayload.getString("hearing.id");


        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

    }


    private Matcher[] getMatcherForCpsOrganisation() {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01"))
        };
    }

    private void doVerifyDefendantListingStatusChangedPayload(final JsonPath defendantListingStatusChangedPayload, final String expectedOffenceId) {
        final String unscheduledHearingId = defendantListingStatusChangedPayload.getString("hearing.id");
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
        assertThat(offences.get(0).get("id"), is(expectedOffenceId));

        final List<HashMap> judicialResults = defendantListingStatusChangedPayload.getJsonObject("hearing.prosecutionCases[0].defendants[0].offences[0].judicialResults");
        assertThat(judicialResults, is(nullValue()));

    }

    private void doVerifyRecordedEventPayload(final JsonPath recordedEventPayload, final String existingHearingId, final String unscheduledHearingId) {
        assertThat(recordedEventPayload.getString("hearingId"), is(existingHearingId));
        List<String> unscheduledHearingIds = recordedEventPayload.getJsonObject("unscheduledHearingIds");
        assertThat(unscheduledHearingIds.size(), is(1));
        assertThat(unscheduledHearingIds.get(0), is(unscheduledHearingId));
    }

    private String prepareHearingForTest() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        String hearingIdInResponse = doVerifyProsecutionCaseDefendantListingStatusChanged(consumerForDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingIdInResponse, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForCasesReferredToCourts();
        return hearingIdInResponse;
    }

    private String prepareHearingForTestWithInitiate() throws Exception {

        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        String hearingIdInResponse = doVerifyProsecutionCaseDefendantListingStatusChanged(consumerForDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingIdInResponse, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyInMessagingQueueForCasesReferredToCourts();
        return hearingIdInResponse;
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumer) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
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

    private void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private static void doVerifyEventIsNotRaised(final JmsMessageConsumerClient messageConsumer, final String originalHearingId, final String hearingId) {

        JsonPath message;
        do {
            message = retrieveMessageAsJsonPath(messageConsumer);
        } while (ofNullable(message).isPresent() && hearingIdIsOneOf(message, originalHearingId, hearingId));

        assertFalse(ofNullable(message).isPresent());
    }

    private static boolean hearingIdIsOneOf(JsonPath jsonPath, String h1, String h2) {
        final String s = jsonPath.getString("hearing.id");
        return h1.equals(s) || h2.equals(s);
    }

}
