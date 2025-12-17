package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollCaseHearingTypes;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class InitiateHearingIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED = "public.listing.hearing-unallocated-courtroom-removed";
    private final JmsMessageConsumerClient publicEventsConsumerForOffencesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-offences-changed").getMessageConsumerClient();

    private final JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-cases-referred-to-court").getMessageConsumerClient();
    private final JmsMessageConsumerClient messageConsumerClientPublicForHearingUnallocatedCourtroomRemoved = newPublicJmsMessageConsumerClientProvider().withEventNames("public.listing.hearing-unallocated-courtroom-removed").getMessageConsumerClient();

    @Test
    public void shouldInitiateHearing() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject(caseId, hearingId, defendantId, courtCentreId);

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

        verifyPublicEventForCasesReferredToCourts();

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endorsableFlag", equalTo(true)));

        verifyCaseHearingTypes(caseId, LocalDate.now());
        ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, randomUUID().toString());        // when
        helper.updateOffences();
        helper.verifyInMessagingQueueForOffencesUpdated(publicEventsConsumerForOffencesUpdated);
    }


    @Test
    public void shouldInitiateHearingForCrownCourt() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject(caseId, hearingId, defendantId, courtCentreId, "CROWN");

        sendPublicEvent(PUBLIC_LISTING_HEARING_CONFIRMED, metadata, hearingConfirmedJson);

        verifyPublicEventForCasesReferredToCourts();

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endorsableFlag", equalTo(true)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].id", equalTo(hearingId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].hearingListingStatus", equalTo("HEARING_INITIALISED")));


        final Metadata metadata2 = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED)
                .withUserId(userId)
                .build();

        final String  payloadString = """
                {"hearingId":"%s", "estimatedMinutes": 30}
                """.formatted(hearingId);
        final JsonObject payload = new StringToJsonObjectConverter().convert(
                payloadString);

        sendPublicEvent(PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED, metadata2, payload);

        verifyPublicEventForHearingUnallocated();

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].endorsableFlag", equalTo(true)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].id", equalTo(hearingId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].hearingListingStatus", equalTo("SENT_FOR_LISTING"))
                );
    }

    private void sendPublicEvent(final String eventName, final Metadata metadata, final JsonObject hearingConfirmedJson) {
        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(metadata, hearingConfirmedJson));
    }

    private JsonObject getHearingJsonObject(final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        return getHearingJsonObject(caseId, hearingId, defendantId, courtCentreId, "MAGISTRATES");
    }

    private JsonObject getHearingJsonObject(final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId,
                                            final String jurisdictionType) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("\"jurisdictionType\": \"MAGISTRATES\"", "\"jurisdictionType\": \"" + jurisdictionType + "\"")
        );
    }

    private void verifyPublicEventForCasesReferredToCourts() {
        Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    private void verifyPublicEventForHearingUnallocated() {
        Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingUnallocatedCourtroomRemoved);
        assertTrue(message.isPresent());
    }

    private void verifyCaseHearingTypes(final String caseId, final LocalDate orderDate) {
        pollCaseHearingTypes(caseId, orderDate.toString(),
                withJsonPath("$.hearingTypes.length()", is(1)),
                withJsonPath("$.hearingTypes[0].hearingId", is(notNullValue())),
                withJsonPath("$.hearingTypes[0].type", is(notNullValue()))
        );
    }
}

