package uk.gov.moj.cpp.progression.event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.JsonObject;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.event.DefendantAllocationDecisionUpdatedListener.PUBLIC_STRUCTURE_EVENTS_DEFENDANT_ALLOCATION_DECISION_UPDATED;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;

@RunWith(MockitoJUnitRunner.class)
public class DefendantAllocationDecisionUpdatedListenerTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(JsonObject.class);

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @InjectMocks
    private DefendantAllocationDecisionUpdatedListener listener;


    @Test
    public void shouldHandleAllocationDecisionUpdatedEvent() throws Exception {
        // given
        String caseId = randomUUIDStr();
        String defendantId = randomUUIDStr();
        JsonEnvelope jsonEnvelope = envelope()
                .with(JsonObjectMetadata.metadataWithRandomUUID("progression.events.defendant-allocation-decision-updated"))
                .withPayloadOf(caseId, CASE_ID)
                .withPayloadOf(defendantId, DEFENDANT_ID)
                .build();

        // when
        listener.handleAllocationDecisionUpdatedEvent(jsonEnvelope);

        // then
        verify(enveloper).withMetadataFrom(eq(jsonEnvelope), eq(PUBLIC_STRUCTURE_EVENTS_DEFENDANT_ALLOCATION_DECISION_UPDATED));
        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEventPayload = envelopeCaptor.getValue();
        assertThat(publicEventPayload.metadata(), withMetadataEnvelopedFrom(jsonEnvelope).withName(PUBLIC_STRUCTURE_EVENTS_DEFENDANT_ALLOCATION_DECISION_UPDATED));

        final JsonObject payload = publicEventPayload.payloadAsJsonObject();
        assertThat(payload.getString(CASE_ID), is(caseId));
        assertThat(payload.getString(DEFENDANT_ID), is(defendantId));
    }

    private String randomUUIDStr() {
        return UUID.randomUUID().toString();
    }
}