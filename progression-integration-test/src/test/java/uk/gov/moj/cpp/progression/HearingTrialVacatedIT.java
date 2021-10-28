package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import com.jayway.restassured.path.json.JsonPath;
import javax.json.Json;
import org.junit.Assert;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingTrialVacatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createConsumer("progression.events.hearing-populated-to-probation-caseworker");
    private static final MessageConsumer messageConsumerHearingTrialVacated = privateEvents.createConsumer("progression.event.hearing-trial-vacated");
    private String vacatedTrialReasonId;

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        vacatedTrialReasonId = UUID.randomUUID().toString();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageConsumerHearingTrialVacated.close();
    }


    @Test
    public void shouldSetVacatedTrialReasonIdToTrue() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

         metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_TRIAL_VACATED)
                .withUserId(userId)
                .build();

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.hearing.trial-vacated.json",hearingId, vacatedTrialReasonId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_TRIAL_VACATED, vacatedTrialObject, metadata);

        verifyInMessagingQueueForHearingTrialvacated(hearingId, vacatedTrialReasonId);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, true);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(true))
        );

        final String updatedCourtCentreId = randomUUID().toString();
        final Metadata hearingUpdatedMetadata = createMetadata(PUBLIC_LISTING_HEARING_UPDATED);
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, caseId, defendantId, updatedCourtCentreId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_UPDATED, hearingUpdatedJson, hearingUpdatedMetadata);

        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, true, updatedCourtCentreId);
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.courtCentre.id", is(updatedCourtCentreId)),
                withJsonPath("$.hearing.isVacatedTrial", is(true))
        );
    }


    @Test
    public void shouldSetVacatedTrialReasonIdToTrueWhenTrialVacatedFromListing() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("public.listing.vacated-trial-updated")
                .withUserId(userId)
                .build();

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.listing.trial-vacated-updated.json",hearingId, vacatedTrialReasonId);

        sendMessage(messageProducerClientPublic,
                "public.listing.vacated-trial-updated", vacatedTrialObject, metadata);

        verifyInMessagingQueueForHearingTrialvacated(hearingId, vacatedTrialReasonId);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, true);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(true))
        );
    }

    @Test
    public void shouldSetVacatedTrialReasonToFalseWhenTrialVacatedFromListing() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("public.listing.vacated-trial-updated")
                .withUserId(userId)
                .build();

        final JsonObject vacatedTrialObject = Json.createObjectBuilder().add("hearingId", hearingId).add("allocated", true).add("isVacated", false).build();

        sendMessage(messageProducerClientPublic,
                "public.listing.vacated-trial-updated", vacatedTrialObject, metadata);

        verifyInMessagingQueueForHearingTrialvacated(hearingId, null);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, false);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );
    }

    @Test
    public void shouldSetVacatedTrialReasonIdToFalse() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_TRIAL_VACATED)
                .withUserId(userId)
                .build();

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.hearing.trial-not-vacated.json",hearingId, null);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_TRIAL_VACATED, vacatedTrialObject, metadata);

        verifyInMessagingQueueForHearingTrialvacated(hearingId, null);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, false);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );

        final String updatedCourtCentreId = randomUUID().toString();
        final Metadata hearingUpdatedMetadata = createMetadata(PUBLIC_LISTING_HEARING_UPDATED);
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, caseId, defendantId, updatedCourtCentreId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_UPDATED, hearingUpdatedJson, hearingUpdatedMetadata);

        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, false, updatedCourtCentreId);
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.courtCentre.id", is(updatedCourtCentreId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );

    }



    private static void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId, final Boolean isVacatedTrial) {
        final JsonPath result = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", CoreMatchers.is(isVacatedTrial))
        )));
        Assert.assertNotNull(result);
    }

    private static void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId, final Boolean isVacatedTrial, final String courtCenterId) {
        final JsonPath result = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", CoreMatchers.is(isVacatedTrial)),
                withJsonPath("$.hearing.courtCentre.id", CoreMatchers.is(courtCenterId))
        )));
        Assert.assertNotNull(result);
    }


    private static void verifyInMessagingQueueForHearingTrialvacated(final String hearingId, final String vacatedTrialReasonId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingTrialVacated);
        assertTrue(message.isPresent());
        final JsonObject jsonObject = message.get();
        assertThat(jsonObject.getString("hearingId"), CoreMatchers.is(hearingId));
        if(nonNull(vacatedTrialReasonId)) {
            assertThat(jsonObject.getString("vacatedTrialReasonId"), CoreMatchers.is(vacatedTrialReasonId));
        }

    }


    private JsonObject getHearingJsonObject(final String path, final String hearingId, final String vacatedTrialReasonId){
        if(nonNull(vacatedTrialReasonId)) {
            final String strPayload = getPayload(path)
                    .replaceAll("HEARING_ID", hearingId)
                    .replaceAll("REASON_ID", vacatedTrialReasonId);
            return stringToJsonObjectConverter.convert(strPayload);
        }else {
            final String strPayload = getPayload(path)
                    .replaceAll("HEARING_ID", hearingId);
            return stringToJsonObjectConverter.convert(strPayload);
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

    private Metadata createMetadata(final String eventName) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(randomUUID().toString())
                .build();
    }

    private JsonObject getHearingUpdatedJsonObject(final String hearingId, final String caseId, final String defendantId,  final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }
}
