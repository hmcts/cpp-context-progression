package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingMarkedAsDuplicateIT extends AbstractIT {

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerHearingMarkedAsDuplicate = privateEvents.createConsumer("progression.event.hearing-marked-as-duplicate");
    private static final MessageConsumer messageConsumerHearingMarkedAsDuplicateForCase = privateEvents.createConsumer("progression.event.hearing-marked-as-duplicate-for-case");

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldHearingAsMarkedDuplicate() throws IOException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String duplicateUrn = generateUrn();
        final String urn = generateUrn();
        final String courtCentreId = UUID.randomUUID().toString();

        final String duplicateHearingId = createHearingAndReturnHearingId(caseId, defendantId, duplicateUrn);
        final String hearingId = createHearingAndReturnHearingId(caseId, defendantId, urn);

        final Metadata metadata = metadataBuilder()
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

    }

    private String createHearingAndReturnHearingId(final String caseId, final String defendantId, final String urn) throws IOException {
        addProsecutionCaseToCrownCourt(caseId, defendantId, urn);

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
}
