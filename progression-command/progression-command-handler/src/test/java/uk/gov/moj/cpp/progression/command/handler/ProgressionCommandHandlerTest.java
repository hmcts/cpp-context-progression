package uk.gov.moj.cpp.progression.command.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandHandlerTest {

    private static final UUID CASE_PROGRESSION_ID = randomUUID();

    @Mock
    private EventStream eventStream;
    @Mock
    private JsonEnvelope mappedJsonEnvelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonString jsonString;

    @Mock
    JsonEnvelope envelope;

    @Mock
    EventSource eventSource;

    @Spy
    Enveloper enveloper;

    @Mock
    Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    Object event;

    @InjectMocks
    private ProgressionCommandHandler progressionCommandHandler;

    @Mock
    ProgressionEventFactory progressionEventFactory;

    @Mock
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Test

    public void shouldHandlerAddCaseToCrownCourtCommand() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();

        when(progressionEventFactory.createCaseAddedToCrownCourt(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.addCaseToCrownCourt(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

    }

    @Test

    public void shouldHandlerSendCommittalHearingInformationCommand() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();

        when(progressionEventFactory.createSendingCommittalHearingInformationAdded(command))
                        .thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.sendCommittalHearingInformation(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

    }

    @Test
    public void shouldaddSentenceHearingDate() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createSentenceHearingDateAdded(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.addSentenceHearingDate(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test
    public void shouldCaseToBeAssigned() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createCaseToBeAssignedUpdated(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.updateCaseToBeAssigned(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test
    public void shouldCaseAssignedForReview() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createCaseAssignedForReviewUpdated(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.updateCaseAssignedForReview(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test
    public void shouldHandleCaseReadyForSentenceHearing() throws Exception {
        when(progressionEventFactory.createCaseReadyForSentenceHearing(envelope)).thenReturn(event);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(eventStream);
        when(enveloper.withMetadataFrom(envelope)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(mappedJsonEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(ProgressionCommandHandler.FIELD_CASE_PROGRESSION_ID))
                        .thenReturn(CASE_PROGRESSION_ID.toString());
        progressionCommandHandler.prepareForSentenceHearing(envelope);

        verify(progressionEventFactory).createCaseReadyForSentenceHearing(eq(envelope));
        verify(eventSource).getStreamById(any());
        verify(enveloper).withMetadataFrom(envelope);

        final ArgumentCaptor<Stream> captor = ArgumentCaptor.forClass(Stream.class);
        verify(eventStream).append(captor.capture());
        assertTrue(captor.getValue().findFirst().get().equals(mappedJsonEnvelope));

        verifyNoMoreInteractions(progressionEventFactory);
        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(eventStream);
    }

    // @Test
    public void shouldHandleRequestPsrForDefendants() throws Exception {
        // TODO: Implement..
        //     : Refactor out some common verification assertion groups..

        progressionCommandHandler.requestPsrForDefendants(envelope);

        verify(progressionEventFactory).createPsrForDefendantsRequested(eq(envelope));
        verify(eventSource).getStreamById(any());
        verify(enveloper).withMetadataFrom(envelope);

    }

    private JsonEnvelope createJsonCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder()
                        .add(ID, UUID.randomUUID().toString()).add(NAME, "SomeName").build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                        .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).build();

        return DefaultJsonEnvelope.envelopeFrom(
                        JsonObjectMetadata.metadataFrom(metadataAsJsonObject), payloadAsJsonObject);

    }

    private class StubEventStream implements EventStream {

        private Stream<JsonEnvelope> events;

        @Override
        public Stream<JsonEnvelope> read() {
            return events;
        }

        @Override
        public Stream<JsonEnvelope> readFrom(final Long version) {
            return events;
        }

        @Override
        public void append(final Stream<JsonEnvelope> events) throws EventStreamException {
            this.events = events;
        }

        @Override
        public void appendAfter(final Stream<JsonEnvelope> events, final Long version)
                        throws EventStreamException {
            this.events = events;
        }

        @Override
        public Long getCurrentVersion() {
            return null;
        }

        @Override
        public UUID getId() {
            return null;
        }
    }
}
