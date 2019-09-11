package uk.gov.moj.cpp.progression.listener;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceOrganisationAssociatedEventProcessorTest {

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @InjectMocks
    private DefenceOrganisationAssociatedEventProcessor defenceOrganisationAssociatedEventProcessor;

    @Test
    public void shouldAssociateDefenceOrganisation() {
        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();

        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.progression.defence-organisation-associated")
                .withUserId(userId.toString());

        final JsonEnvelope event = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(defendantId.toString(), "defendantId").build();

        defenceOrganisationAssociatedEventProcessor.processEvent(event);

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertEquals("public.progression.defence-organisation-associated", envelopeArgumentCaptor.getValue().metadata().name());
        assertEquals(defendantId.toString(), envelopeArgumentCaptor.getValue().payload().getString("defendantId"));
    }
}