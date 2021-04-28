package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingDeletedIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED = "public.events.listing.allocated-hearing-deleted";
    private static final String PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED = "public.events.listing.unallocated-hearing-deleted";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerHearingDeleted = privateEvents.createConsumer("progression.event.hearing-deleted");
    private static final MessageConsumer messageConsumerHearingDeletedForProsecutionCase = privateEvents.createConsumer("progression.event.hearing-deleted-for-prosecution-case");


    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldDeleteHearingWhenHandlingAllocatedHearingDeleted() throws IOException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String urn = generateUrn();
        final String hearingId = createHearingAndReturnHearingId(caseId, defendantId, urn);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED)
                .withUserId(userId)
                .build();

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED, hearingDeletedJson, metadata);

        verifyInMessagingQueueForHearingDeleted();
        verifyInMessagingQueueForHearingDeletedForProsecutionCase();
        verifyHearingIsEmpty(hearingId);
    }

    @Test
    public void shouldDeleteHearingWhenHandlingUnallocatedHearingDeleted() throws IOException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String urn = generateUrn();
        final String hearingId = createHearingAndReturnHearingId(caseId, defendantId, urn);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED)
                .withUserId(userId)
                .build();

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED, hearingDeletedJson, metadata);

        verifyInMessagingQueueForHearingDeleted();
        verifyInMessagingQueueForHearingDeletedForProsecutionCase();
        verifyHearingIsEmpty(hearingId);
    }

    private String createHearingAndReturnHearingId(final String caseId, final String defendantId, final String urn) throws IOException {
        addProsecutionCaseToCrownCourt(caseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
    }

    private static void verifyInMessagingQueueForHearingDeleted() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingDeleted);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForHearingDeletedForProsecutionCase() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingDeletedForProsecutionCase);
        assertTrue(message.isPresent());
    }
}
