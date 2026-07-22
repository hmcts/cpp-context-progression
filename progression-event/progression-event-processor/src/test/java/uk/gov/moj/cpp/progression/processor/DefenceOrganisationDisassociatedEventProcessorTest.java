package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefenceOrganisationDisassociatedEventProcessorTest {

    private final UUID userId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID organisationId = randomUUID();
    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;
    @Mock
    private Sender sender;
    @Inject
    private Enveloper enveloper;
    @InjectMocks
    private DefenceOrganisationDisassociatedEventProcessor defenceOrganisationDisassociatedEventProcessor;

    @Test
    public void shouldDisassociateDefenceOrganisation() {
        final JsonEnvelope event = generateDisassociatedEvent(userId, defendantId, organisationId);

        defenceOrganisationDisassociatedEventProcessor.processEvent(event);

        verifyPublicEvent();
    }

    private void verifyPublicEvent() {
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat( envelopeArgumentCaptor.getValue().metadata().name(), is("public.defence.defence-organisation-disassociated"));
        final JsonObject capturedPayload = envelopeArgumentCaptor.getValue().payload();
        assertThat(envelopeArgumentCaptor.getValue().payload().getString("userId"), is(userId.toString()));
        assertThat(capturedPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(envelopeArgumentCaptor.getValue().payload().getString("organisationId"), is(organisationId.toString()));
    }

    private JsonEnvelope generateDisassociatedEvent(final UUID userId, final UUID defendantId, final UUID organisationId) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.defence-organisation-disassociated")
                .withUserId(userId.toString());
        final JsonObject payload = createObjectBuilder()
                .add("userId", userId.toString())
                .add("defendantId", defendantId.toString())
                .add("organisationId", organisationId.toString())
                .build();
        return JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);
    }

}