package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiveRepresentationOrderForApplicationApiTest {

    @Mock
    private Sender sender;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private ReceiveRepresentationOrderForApplicationApi receiveRepresentationOrderForApplicationApi;

    @Test
    void shouldReceiveRepresentationOrderForApplicationAPI() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        receiveRepresentationOrderForApplicationApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.receive-representationOrder-for-application"));

    }


    @Test
    void shouldReceiveRepresentationOrderForApplicationAPIWithNoAssociation() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        receiveRepresentationOrderForApplicationApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.receive-representationOrder-for-application"));
    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNull() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNotValidUUID() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", "invalid-uuid")
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNull() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNotValidUUID() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", "invalid-uuid")
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNull() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNotValidUUID() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

}
