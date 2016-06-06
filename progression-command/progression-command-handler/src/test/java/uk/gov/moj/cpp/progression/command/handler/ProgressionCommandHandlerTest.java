package uk.gov.moj.cpp.progression.command.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandHandlerTest {

	private static final UUID CASE_PROGRESSION_ID = UUID.randomUUID();
	private static final String VERSION = "1";
	private static final UUID INDICATE_STATEMENT_ID = UUID.randomUUID();

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

		when(progressionEventFactory.createSendingCommittalHearingInformationAdded(command)).thenReturn(event);
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
		when(progressionEventFactory.createProsecutionTrialEstimateAdded(command)).thenReturn(event);
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

	private JsonEnvelope createJsonCommand() {
		final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString()).add(NAME, "SomeName").build();

		final JsonObject payloadAsJsonObject = Json.createObjectBuilder().add("caseProgressionId", CASE_PROGRESSION_ID.toString())
				.add("indicateStatementId", INDICATE_STATEMENT_ID.toString()).add("version", VERSION.toString()).build();

		return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject), payloadAsJsonObject);

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
		public void appendAfter(Stream<JsonEnvelope> events, Long version) throws EventStreamException {
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