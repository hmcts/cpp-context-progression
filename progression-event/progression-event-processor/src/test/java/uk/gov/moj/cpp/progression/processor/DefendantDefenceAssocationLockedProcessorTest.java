package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDefenceAssocationLockedProcessorTest {

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @InjectMocks
    private DefendantDefenceAssociationLockedProcessor defendantDefenceAssociationLockedProcessor;


    @Test
    public void shouldAssociateDefenceOrganisation() {

        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("public.progression.defence-association-for-laa-locked")
                .withUserId(userId.toString());

        final JsonObject payload = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .add("lockedByRepOrder", true)
                .build();

        JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);

        defendantDefenceAssociationLockedProcessor.processEvent(event);
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("public.progression.defence-association-for-laa-locked"));

        JsonObject capturedPayload = envelopeArgumentCaptor.getValue().payload();

        assertThat(capturedPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(envelopeArgumentCaptor.getValue().payload().getString("prosecutionCaseId"), is(prosecutionCaseId.toString()));
        assertThat(envelopeArgumentCaptor.getValue().payload().getBoolean("lockedByRepOrder"), is(true));
    }
}