package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.events.RepresentationType;

import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefenceOrganisationAssociatedEventProcessorTest {

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @InjectMocks
    private DefenceOrganisationAssociatedEventProcessor defenceOrganisationAssociatedEventProcessor;

    private static final String ORGANISATION_NAME = "CompanyZ";

    @Test
    public void shouldAssociateDefenceOrganisation() {

        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();

        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.progression.defence-organisation-associated")
                .withUserId(userId.toString());

        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("organisationId", organisationId.toString())
                .add("organisationName", ORGANISATION_NAME)
                .add("representationType", RepresentationType.REPRESENTATION_ORDER.toString())
                .build();

        JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);

        defenceOrganisationAssociatedEventProcessor.processEvent(event);
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertEquals("public.progression.defence-organisation-associated", envelopeArgumentCaptor.getValue().metadata().name());

        JsonObject capturedPayload = envelopeArgumentCaptor.getValue().payload();
        assertEquals(defendantId.toString(), capturedPayload.getString("defendantId"));
        assertEquals(organisationId.toString(), envelopeArgumentCaptor.getValue().payload().getString("organisationId"));
        assertEquals(ORGANISATION_NAME, envelopeArgumentCaptor.getValue().payload().getString("organisationName"));
        assertEquals(RepresentationType.REPRESENTATION_ORDER.toString(), envelopeArgumentCaptor.getValue().payload().getString("representationType"));
    }
}