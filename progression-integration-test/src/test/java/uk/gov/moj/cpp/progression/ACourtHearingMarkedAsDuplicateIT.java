package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import com.jayway.restassured.path.json.JsonPath;
import org.junit.Assert;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ACourtHearingMarkedAsDuplicateIT extends AbstractIT {

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerHearingMarkedAsDuplicate = privateEvents.createConsumer("progression.event.hearing-marked-as-duplicate");
    private static final MessageConsumer messageConsumerHearingMarkedAsDuplicateForCase = privateEvents.createConsumer("progression.event.hearing-marked-as-duplicate-for-case");
    private static final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createConsumer("progression.events.hearing-populated-to-probation-caseworker");

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
    }

    @Test
    public void shouldHearingAsMarkedDuplicate() throws IOException, JMSException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String duplicateUrn = generateUrn();
        final String urn = generateUrn();
        final String courtCentreId = UUID.randomUUID().toString();

        final String duplicateHearingId = createHearingAndReturnHearingId(caseId, defendantId, duplicateUrn);
        final String hearingId = createHearingAndReturnHearingId(caseId, defendantId, urn);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, duplicateHearingId, defendantId, courtCentreId, "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT)
                .withUserId(userId)
                .build();

        final JsonObject hearingMarkedAsDuplicateJson = getHearingMarkedAsDuplicateObject(caseId, duplicateHearingId, defendantId, courtCentreId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, hearingMarkedAsDuplicateJson, metadata);

        verifyInMessagingQueueForHearingMarkedAsDuplicate();
        verifyInMessagingQueueForHearingMarkedAsDuplicateForCase();
        verifyHearingIsEmpty(duplicateHearingId);
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.hearingsAtAGlance.defendantHearings[0].hearingIds[*]", hasSize(1)));
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.hearingsAtAGlance.defendantHearings[0].hearingIds[0]", equalTo(hearingId)));

        final JsonPath messageDaysMatchers = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(withJsonPath("$.hearing.id", is(duplicateHearingId))));
        Assert.assertNotNull(messageDaysMatchers);
    }

    private String createHearingAndReturnHearingId(final String caseId, final String defendantId, final String urn) throws IOException {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
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

    private static void verifyInMessagingQueueForHearingMarkedAsDuplicate() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingMarkedAsDuplicate);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForHearingMarkedAsDuplicateForCase() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingMarkedAsDuplicateForCase);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingPopulatedToProbationCaseWorker);
        assertTrue(message.isPresent());
        final JsonObject jsonObject = message.get();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearingId));

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
}
