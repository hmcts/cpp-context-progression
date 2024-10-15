package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private FormApi formApi;


    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Test
    public void shouldHandleCreateForm() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID id = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("id", id.toString())
                .add("formId", formId.toString())
                .add("formType", FormType.BCM.name())
                .add("formDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .add("formData", createObjectBuilder().build().toString()).build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.create-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        formApi.createForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.create-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }


    @Test
    public void shouldHandleUpdateForm() {
        final JsonObject payload = createObjectBuilder()
                .add("formData", createObjectBuilder().build())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.update-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        formApi.updateForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.update-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }


    @Test
    public void shouldHandleFinaliseForm() {
        final JsonObject payload = createObjectBuilder()
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.finalise-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        formApi.finaliseForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.finalise-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

    @Test
    public void shouldHandleUpdateBcmDefendants() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonObject payload = createObjectBuilder()
                .add("formType", FormType.BCM.name())
                .add("formDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId.toString())
                        .build()))
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.update-form-defendants")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        formApi.updateBcmDefendants(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.update-form-defendants"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }



    @Test
    public void shouldHandleEditForm() {
        final JsonObject payload = createObjectBuilder()
                .add("userId", randomUUID().toString())
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.request-edit-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        formApi.requestEditForm(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.request-edit-form"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }

}