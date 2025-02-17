package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingsForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyListUnscheduledHearingRequestsAsStreamV2;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.Utilities.listenForPrivateEvent;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingResultedUnscheduledListingIT extends AbstractIT {
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 = "public.events.hearing.hearing-resulted-unscheduled-listing";
    private static final String PUBLIC_HEARING_RESULTED_WITH_APPLICATION_RESULT_UNSCHEDULED_LISTING_V2 = "public.events.hearing.hearing-resulted-unscheduled-listing-with-application-resulted";

    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private final JmsMessageConsumerClient consumerForUnscheduledHearingRecorded = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.unscheduled-hearing-recorded").getMessageConsumerClient();

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-cases-referred-to-court").getMessageConsumerClient();
    public static final String EXPECTED_OFFENCE_ID = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;

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

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(consumerForUnscheduledHearingRecorded)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final String unscheduledHearingId = pollCaseAndGetHearingsForDefendant(caseId, defendantId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='" + defendantId + "')].hearingIds[*]", hasSize(greaterThan(1))))
                .stream().filter(s -> ! s.equals(existingHearingId)).findFirst().get();
        getHearingForDefendant(unscheduledHearingId, new Matcher[]{withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(EXPECTED_OFFENCE_ID))});

        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event

        final JsonEnvelope publicEventEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope2);
        final String unscheduledHearingId2 = pollCaseAndGetHearingsForDefendant(caseId, defendantId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='" + defendantId + "')].hearingIds[*]", hasSize(greaterThan(2))))
                .stream().filter(s -> ! s.equals(existingHearingId)).filter(s -> ! s.equals(unscheduledHearingId)).findFirst().get();

        getHearingForDefendant(unscheduledHearingId2, new Matcher[]{withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(EXPECTED_OFFENCE_ID))});

        verifyListUnscheduledHearingRequestsAsStreamV2(unscheduledHearingId, "1 week");
    }

    @Test
    public void shouldKeepsCpsOrganisationAndListUnscheduledHearingsV2() throws Exception {
        final String existingHearingId = prepareHearingForTestWithInitiate();

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(consumerForUnscheduledHearingRecorded)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final String unscheduledHearingId = pollCaseAndGetHearingsForDefendant(caseId, defendantId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='" + defendantId + "')].hearingIds[*]", hasSize(greaterThan(1))))
                .stream().filter(s -> ! s.equals(existingHearingId)).findFirst().get();
        getHearingForDefendant(unscheduledHearingId, new Matcher[]{withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(EXPECTED_OFFENCE_ID))});


        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

        //amendment/resharing: should not raise any event

        final JsonEnvelope publicEventEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_UNSCHEDULED_LISTING_V2 + ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope2);

        final String unscheduledHearingId2 = pollCaseAndGetHearingsForDefendant(caseId, defendantId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='" + defendantId + "')].hearingIds[*]", hasSize(greaterThan(2))))
                .stream().filter(s -> ! s.equals(existingHearingId)).filter(s -> ! s.equals(unscheduledHearingId)).findFirst().get();
        getHearingForDefendant(unscheduledHearingId2, new Matcher[]{withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(EXPECTED_OFFENCE_ID))});


        pollProsecutionCasesProgressionFor(caseId, getMatcherForCpsOrganisation());

        verifyListUnscheduledHearingRequestsAsStreamV2(unscheduledHearingId, "1 week");
    }

    @SuppressWarnings("squid:S1607")
    @Test
    public void shouldListUnscheduledHearingsV2WhenApplicationResultedWithCase() throws Exception {
        final String existingHearingId = prepareHearingForTest();

        Utilities.EventListener eventListenerForHearingRecorded = listenForPrivateEvent(consumerForUnscheduledHearingRecorded)
                .withFilter(isJson(withJsonPath("$.hearingId", is(existingHearingId))));

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject( PUBLIC_HEARING_RESULTED_WITH_APPLICATION_RESULT_UNSCHEDULED_LISTING_V2+ ".json", caseId,
                existingHearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final String unscheduledHearingId = pollCaseAndGetHearingsForDefendant(caseId, defendantId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='" + defendantId + "')].hearingIds[*]", hasSize(greaterThan(1))))
                .stream().filter(s -> ! s.equals(existingHearingId)).findFirst().get();
        getHearingForDefendant(unscheduledHearingId, new Matcher[]{withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(EXPECTED_OFFENCE_ID))});



        final JsonPath recordedEventPayload = eventListenerForHearingRecorded.waitFor();
        doVerifyRecordedEventPayload(recordedEventPayload, existingHearingId, unscheduledHearingId);

    }


    private Matcher[] getMatcherForCpsOrganisation() {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01"))
        };
    }

    private void doVerifyRecordedEventPayload(final JsonPath recordedEventPayload, final String existingHearingId, final String unscheduledHearingId) {
        assertThat(recordedEventPayload.getString("hearingId"), is(existingHearingId));
        List<String> unscheduledHearingIds = recordedEventPayload.getJsonObject("unscheduledHearingIds");
        assertThat(unscheduledHearingIds.size(), is(1));
        assertThat(unscheduledHearingIds.get(0), is(unscheduledHearingId));
    }

    private String prepareHearingForTest() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String hearingIdInResponse = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingIdInResponse, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyPublicEventCasesReferredToCourts();
        return hearingIdInResponse;
    }

    private String prepareHearingForTestWithInitiate() throws Exception {

        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        String hearingIdInResponse = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingIdInResponse, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyPublicEventCasesReferredToCourts();
        return hearingIdInResponse;
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

    private void verifyPublicEventCasesReferredToCourts() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

}
