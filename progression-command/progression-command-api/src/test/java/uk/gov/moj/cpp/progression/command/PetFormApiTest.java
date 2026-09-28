package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PetFormApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private PetFormApi petFormApi;


    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Test
    public void shouldHandleCreatePetForm() {
        final JsonObject payload = createObjectBuilder()
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.create-pet-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        petFormApi.createPetForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.create-pet-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }


    @Test
    public void shouldHandleFinalisePetForm() {
        final JsonObject payload = createObjectBuilder()
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.finalise-pet-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        petFormApi.finalisePetForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.finalise-pet-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    @Test
    public void shouldHandleUpdatePetForm() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", createObjectBuilder().build())
                .add("petFormData", createObjectBuilder().build())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.update-pet-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        petFormApi.updatePetForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.update-pet-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    @Test
    public void shouldHandleUpdatePetDetail() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", createObjectBuilder().build())
                .add("petFormData", createObjectBuilder().build())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.update-pet-detail")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        petFormApi.updatePetDetail(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.update-pet-detail"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    @Test
    public void shouldHandleUpdatePetFormForDefendant() {
        final JsonObject payload = createObjectBuilder()
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.update-pet-form-for-defendant")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        petFormApi.updatePetFormForDefendant(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.update-pet-form-for-defendant"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    @Test
    public void shouldHandleReleasePetForm() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", createObjectBuilder().build())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.release-pet-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        petFormApi.releasePetForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.release-pet-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

}