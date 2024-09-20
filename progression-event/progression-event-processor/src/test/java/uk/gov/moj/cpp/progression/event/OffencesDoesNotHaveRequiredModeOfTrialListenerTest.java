package uk.gov.moj.cpp.progression.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.event.OffencesDoesNotHaveRequiredModeOfTrialListener.STRUCTURE_EVENTS_DEFENDANT_OFFENCES_DOES_NOT_HAVE_REQUIRED_MODEOFTRIAL;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @deprecated This is deprecated for Release 2.4
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ExtendWith(MockitoExtension.class)
public class OffencesDoesNotHaveRequiredModeOfTrialListenerTest {
    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(JsonObject.class);

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @InjectMocks
    private OffencesDoesNotHaveRequiredModeOfTrialListener listener;


    @Test
    public void shouldHandleOffencesDoesNotHaveRequiredModeOfTrialEvent() throws Exception {
        // given
        final String caseId = randomUUIDStr();
        final String defendantId = randomUUIDStr();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataWithRandomUUID("progression.events.defendant-offences-does-not-have-required-modeoftrial"))
                .withPayloadOf(caseId, CASE_ID)
                .withPayloadOf(defendantId, DEFENDANT_ID)
                .build();

        // when
        listener.handleOffencesDoesNotHaveRequiredModeOfTrialEvent(jsonEnvelope);

        // then
        verify(enveloper).withMetadataFrom(eq(jsonEnvelope), eq(STRUCTURE_EVENTS_DEFENDANT_OFFENCES_DOES_NOT_HAVE_REQUIRED_MODEOFTRIAL));
        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEventPayload = envelopeCaptor.getValue();
        assertThat(publicEventPayload.metadata(), withMetadataEnvelopedFrom(jsonEnvelope).withName(STRUCTURE_EVENTS_DEFENDANT_OFFENCES_DOES_NOT_HAVE_REQUIRED_MODEOFTRIAL));

        final JsonObject payload = publicEventPayload.payloadAsJsonObject();
        assertThat(payload.getString(CASE_ID), is(caseId));
        assertThat(payload.getString(DEFENDANT_ID), is(defendantId));
    }

    private String randomUUIDStr() {
        return UUID.randomUUID().toString();
    }
}