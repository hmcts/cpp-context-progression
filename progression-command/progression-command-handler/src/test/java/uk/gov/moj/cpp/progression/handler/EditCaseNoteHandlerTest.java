package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.CaseNoteEditedV2;
import uk.gov.justice.core.courts.EditCaseNote;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EditCaseNoteHandlerTest {
    @InjectMocks
    private EditCaseNoteHandler editCaseNoteHandler;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventSource eventSource;

    @Mock
    private CaseAggregate caseAggregate;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseNoteEditedV2.class);

    @BeforeEach
    public void setup() {
        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldHandleEditCaseNote() throws EventStreamException {
        final EditCaseNote editCaseNote = EditCaseNote.editCaseNote()
                .withCaseId(UUID.randomUUID())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.edit-case-note")
                .withId(randomUUID())
                .build();

        final Envelope<EditCaseNote> envelope = envelopeFrom(metadata, editCaseNote);
        editCaseNoteHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  defendantPartialMatchCreatedEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.case-note-edited-v2")).findFirst().get();

        MatcherAssert.assertThat(defendantPartialMatchCreatedEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}
