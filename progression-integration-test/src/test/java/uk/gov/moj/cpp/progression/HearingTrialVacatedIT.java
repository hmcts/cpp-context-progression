package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingTrialVacatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingTrialVacated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-trial-vacated").getMessageConsumerClient();
    public static final String PUBLIC_LISTING_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    private String vacatedTrialReasonId;

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
        vacatedTrialReasonId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldSetVacatedTrialReasonIdToTrue() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY")))));

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();


        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.hearing.trial-vacated.json", hearingId, vacatedTrialReasonId);

        final JsonEnvelope publicEventEnvelope2 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_TRIAL_VACATED, userId), vacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_TRIAL_VACATED, publicEventEnvelope2);

        verifyInMessagingQueueForHearingTrialvacated(hearingId, vacatedTrialReasonId);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, true);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(true))
        );

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, caseId, defendantId, updatedCourtCentreId);
        final JsonEnvelope publicEventEnvelope3 = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, randomUUID()), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope3);

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

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventConfirmedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.listing.trial-vacated-updated.json", hearingId, vacatedTrialReasonId);

        final JsonEnvelope publicEventUpdatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, userId), vacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, publicEventUpdatedEnvelope);


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

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject vacatedTrialObject = Json.createObjectBuilder().add("hearingId", hearingId).add("allocated", true).add("isVacated", false).build();

        final JsonEnvelope publicEventUpdatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, userId), vacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, publicEventUpdatedEnvelope);

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

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.hearing.trial-not-vacated.json", hearingId, null);

        final JsonEnvelope publicEventVacatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_TRIAL_VACATED, userId), vacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_TRIAL_VACATED, publicEventVacatedEnvelope);

        verifyInMessagingQueueForHearingTrialvacated(hearingId, null);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, false);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, caseId, defendantId, updatedCourtCentreId);

        final JsonEnvelope publicEventUpdatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, randomUUID()), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventUpdatedEnvelope);

        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, false, updatedCourtCentreId);
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.courtCentre.id", is(updatedCourtCentreId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );

    }


    private void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId, final Boolean isVacatedTrial) {
        final JsonPath result = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", CoreMatchers.is(isVacatedTrial))
        )));
        assertThat(result, notNullValue());
    }

    private void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId, final Boolean isVacatedTrial, final String courtCenterId) {
        final JsonPath result = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", CoreMatchers.is(isVacatedTrial)),
                withJsonPath("$.hearing.courtCentre.id", CoreMatchers.is(courtCenterId))
        )));
        assertThat(result, notNullValue());
    }


    private void verifyInMessagingQueueForHearingTrialvacated(final String hearingId, final String vacatedTrialReasonId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingTrialVacated);
        assertThat(message.isPresent(), is(true));
        final JsonObject jsonObject = message.get();
        assertThat(jsonObject.getString("hearingId"), CoreMatchers.is(hearingId));
        if (nonNull(vacatedTrialReasonId)) {
            assertThat(jsonObject.getString("vacatedTrialReasonId"), CoreMatchers.is(vacatedTrialReasonId));
        }

    }


    private JsonObject getHearingJsonObject(final String path, final String hearingId, final String vacatedTrialReasonId) {
        if (nonNull(vacatedTrialReasonId)) {
            final String strPayload = getPayload(path)
                    .replaceAll("HEARING_ID", hearingId)
                    .replaceAll("REASON_ID", vacatedTrialReasonId);
            return stringToJsonObjectConverter.convert(strPayload);
        } else {
            final String strPayload = getPayload(path)
                    .replaceAll("HEARING_ID", hearingId);
            return stringToJsonObjectConverter.convert(strPayload);
        }
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
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

    private JsonObject getHearingUpdatedJsonObject(final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }
}
