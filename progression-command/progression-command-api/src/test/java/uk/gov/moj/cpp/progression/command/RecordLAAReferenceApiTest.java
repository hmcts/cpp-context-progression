package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"squid:S5976"})
@ExtendWith(MockitoExtension.class)
public class RecordLAAReferenceApiTest {

    @Mock
    private Sender sender;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private RecordLAAReferenceApi recordLAAReferenceApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Test
    public void shouldRecordLAAReferenceForOffence() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.handler.record-laareference-for-offence", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        recordLAAReferenceApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.record-laareference-for-offence"));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNotValidUUIDForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offenceId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsnullForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfDefendantIdIsNotValidUUIDForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", "invalid-uuid")
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfDefendantIdIsNullForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfCasedIsNullForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("offenceId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfProsecutionCaseIdIsNotValidUUIDForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .add("prosecutionCaseId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }



    @Test
    public void shouldRecordLAAReferenceForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.handler.record-laareference-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        recordLAAReferenceApi.handleForApplication(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.record-laareference-for-application"));
    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNullForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNotValidUUIDForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", "invalid-uuid")
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNullForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNotValidUUIDForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", "invalid-uuid")
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNullForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNotValidUUIDForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }
}
