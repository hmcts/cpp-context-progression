package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ACourtHearingMarkedAsDuplicateIT extends AbstractIT {

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingMarkedAsDuplicate = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-marked-as-duplicate").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingMarkedAsDuplicateForCase = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.hearing-marked-as-duplicate-for-case").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @Test
    public void shouldHearingAsMarkedDuplicate() throws IOException, JMSException, JSONException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String duplicateUrn = generateUrn();
        final String urn = generateUrn();
        final String courtCentreId = UUID.randomUUID().toString();

        final String duplicateHearingId = createHearingAndReturnHearingId(caseId, defendantId, duplicateUrn);
        final String hearingId = createHearingAndReturnHearingId(caseId, defendantId, urn);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, duplicateHearingId, defendantId, courtCentreId, "Lavender Hill Magistrate's Court");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingMarkedAsDuplicateJson = getHearingMarkedAsDuplicateObject(caseId, duplicateHearingId, defendantId, courtCentreId);

        final JsonEnvelope publicEventDuplicateEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, userId), hearingMarkedAsDuplicateJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, publicEventDuplicateEnvelope);

        verifyInMessagingQueueForHearingMarkedAsDuplicate();
        verifyInMessagingQueueForHearingMarkedAsDuplicateForCase();
        verifyHearingIsEmpty(duplicateHearingId);
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.hearingsAtAGlance.defendantHearings[0].hearingIds[*]", hasSize(1)));
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.hearingsAtAGlance.defendantHearings[0].hearingIds[0]", equalTo(hearingId)));

        final JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(withJsonPath("$.hearing.id", is(duplicateHearingId))));
        Assert.assertNotNull(messageDaysMatchers);
    }

    private String createHearingAndReturnHearingId(final String caseId, final String defendantId, final String urn) throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingMarkedAsDuplicateObject(final String caseId, final String hearingId,
                                                         final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.hearing.marked-as-duplicate.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("OFFENCE_ID", randomUUID().toString())
        );
    }

    private void verifyInMessagingQueueForHearingMarkedAsDuplicate() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingMarkedAsDuplicate);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForHearingMarkedAsDuplicateForCase() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingMarkedAsDuplicateForCase);
        assertTrue(message.isPresent());
    }

    private void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerHearingPopulatedToProbationCaseWorker);
        assertTrue(message.isPresent());
        final JsonObject jsonObject = message.get();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearingId));

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
}
