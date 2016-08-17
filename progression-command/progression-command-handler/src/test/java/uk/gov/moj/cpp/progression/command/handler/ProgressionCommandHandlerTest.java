package uk.gov.moj.cpp.progression.command.handler;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;


@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandHandlerTest {

    private static final UUID CASE_PROGRESSION_ID = randomUUID();
    private static final int VERSION = 1;
    private static final UUID INDICATE_STATEMENT_ID = randomUUID();
    public static final Long FIELD_VERSION_VALUE = 1L;

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
    public void shouldHandlerSendCaseToCrownCourtCommand() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();

        when(progressionEventFactory.createCaseSentToCrownCourt(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.sendToCrownCourt(command);
        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

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
    public void shouldHandlerAddDefenceIssuesCommand() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();

        when(progressionEventFactory.createDefenceIssuesAdded(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.addDefenceIssues(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

    }

    @Test

    public void shouldHandlerAddSfrIssuesCommand() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();

        when(progressionEventFactory.createSfrIssuesAdded(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.addSfrIssues(command);

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

    public void shouldAddDefenceTrialEstimate() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createDefenceTrialEstimateAdded(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.addDefenceTrialEstimate(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

    }

    @Test

    public void shouldAddProsecutionTrialEstimate() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createProsecutionTrialEstimateAdded(command))
                        .thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        progressionCommandHandler.addProsecutionTrialEstimate(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test

    public void shouldIssueDirection() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createDirectionIssued(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        progressionCommandHandler.issueDirection(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test

    public void shouldPreSentenceReport() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createPreSentenceReportOrdered(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.preSentenceReport(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test

    public void shouldIndicatestatement() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createIndicateEvidenceServed(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(INDICATE_STATEMENT_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.indicatestatement(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test
    public void shouldIndicateAllStatementsIdentified() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createAllStatementsIdentified(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.indicateAllStatementsIdentified(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test

    public void shouldIndicateAllStatementsServed() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();

        when(progressionEventFactory.createAllStatementsServed(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.indicateAllStatementsServed(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

    @Test
    public void shouldVacatePTPHearing() throws Exception {
        final JsonEnvelope command = createJsonCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(progressionEventFactory.createPTPHearingVacated(command)).thenReturn(event);
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(event)).thenReturn(envelope);
        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);

        progressionCommandHandler.vacatePTPHearing(command);

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

        ArgumentCaptor<Stream> captor = ArgumentCaptor.forClass(Stream.class);
        verify(eventStream).append(captor.capture());
        assertTrue(captor.getValue().findFirst().get().equals(mappedJsonEnvelope));

        verifyNoMoreInteractions(progressionEventFactory);
        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleDefendantEvent() throws Exception {
        UUID ID = UUID.randomUUID();
        given(envelope.payloadAsJsonObject()).willReturn(jsonObject);
        given(envelope.payloadAsJsonString()).willReturn(jsonString);
        DefendantCommand defendant = mock(DefendantCommand.class);
        given(jsonObjectToObjectConverter.convert(jsonObject, DefendantCommand.class))
                        .willReturn(defendant);
        given(defendant.getDefendantProgressionId()).willReturn(ID);
        given(progressionEventFactory.addDefendantEvent(defendant)).willReturn(event);
        given(eventSource.getStreamById(ID)).willReturn(eventStream);
        given(enveloper.withMetadataFrom(envelope)).willReturn(enveloperFunction);
        given(enveloperFunction.apply(event)).willReturn(mappedJsonEnvelope);

        // when
        progressionCommandHandler.addAdditionalInformationForDefendant(envelope);

        verify(progressionEventFactory).addDefendantEvent(eq(defendant));
        verify(eventSource).getStreamById(any());
        verify(enveloper).withMetadataFrom(envelope);

        ArgumentCaptor<Stream> captor = ArgumentCaptor.forClass(Stream.class);
        verify(eventStream).append(captor.capture());
        assertTrue(captor.getValue().findFirst().get().equals(mappedJsonEnvelope));

        verifyNoMoreInteractions(progressionEventFactory);
        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(eventStream);
    }

    private String randomString() {
        return randomAlphabetic(10);
    }

    private JsonEnvelope createJsonCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder()
                        .add(ID, UUID.randomUUID().toString()).add(NAME, "SomeName").build();

        final JsonObject payloadAsJsonObject =
                        Json.createObjectBuilder()
                                        .add("caseProgressionId", CASE_PROGRESSION_ID.toString())
                                        .add("indicateStatementId",
                                                        INDICATE_STATEMENT_ID.toString())
                                        .add("version", VERSION).build();

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
        public Stream<JsonEnvelope> readFrom(Long version) {
            return events;
        }

        @Override
        public void append(Stream<JsonEnvelope> events) throws EventStreamException {
            this.events = events;
        }

        @Override
        public void appendAfter(Stream<JsonEnvelope> events, Long version)
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
