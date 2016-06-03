package uk.gov.moj.cpp.progression.command.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.moj.cpp.progression.domain.CaseProgressionDetails;
import uk.gov.moj.cpp.progression.domain.IndicateStatement;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandHandlerTest {

	private static final String EVENT_NAME = "progression.event.case-sent-to-crown-court";
	private static final String ADD_CASE_EVENT_NAME = "progression.event.case-added-to-crown-court";
	private static final String SENDING_COMMITTAL_EVENT_NAME = "progression.event.sending-committal-hearing-information-added";
	private static final String DEFENCE_ISSUE_EVENT_NAME = "progression.event.defence-issues-added";
	private static final String DEFENCE_TRIAL_ESTIMATE_EVENT_NAME = "progression.event.defence-trial-estimate-added";
	private static final String PROSECUTION_TRIAL_ESTIMATE_EVENT_NAME = "progression.event.prosecution-trial-estimate-added";
	private static final String SFR_ISSUE_EVENT_NAME = "progression.event.sfr-issues-added";
	private static final String DIRECTION_ISSUED_EVENT_NAME = "progression.event.direction-issued";
	private static final String PRE_SENTENCE_REPORT_EVENT_NAME = "progression.event.pre-sentence-report-ordered";
	private static final String ALL_STATEMENT_IDENTIFIED_EVENT_NAME = "progression.event.all-statements-identified";
	private static final String ALL_STATEMENT_SERVED_EVENT_NAME = "progression.event.all-statements-served";
	private static final UUID CASE_PROGRESSION_ID = UUID.randomUUID();
	private static final UUID CASE_ID = UUID.randomUUID();
	private static final String DEFENCE_ISSUE = "defence issues";
	private static final String SFR_ISSUE = "sfr issues";
	private static final String VERSION = "1";
	private static final String COURT_CENTRE_ID = "courtCentreId";
	private static final String FROM_COURT_CENTRE = "Liverpool";
	private static final LocalDate SENDING_COMMITTAL_DATE = LocalDate.now();
	private static int DEFENCE_TRIAL_ESTIMATE = 1;
	private static int PROSECUTION_TRIAL_ESTIMATE = 2;
	private static final  Boolean IS_PSR_ORDERED = true;
	private static final UUID INDICATE_STATEMENT_ID = UUID.randomUUID();;
    private static final String IS_KEY_EVIDENCE = "true";
    private static final String EVIDENCE_NAME = "main evidence";
    private static final LocalDate PLAN_DATE = LocalDate.now();

	@Mock
	JsonEnvelope envelope;

	@Mock
	EventSource eventSource;

	@Mock
	Enveloper enveloper;

	@Mock
	Function<Object, JsonEnvelope> enveloperFunction;

	@Mock
	Stream<Object> events;

	@Mock
	AggregateService aggregateService;

	@Mock
	CaseProgressionDetails caseProgressionDetails;
	
	@Mock
    IndicateStatement indicateStatement;

	@InjectMocks
	private ProgressionCommandHandler progressionCommandHandler;

	@Test
	public void shouldHandlerSendCaseToCrownCourtCommand() throws Exception {
		final JsonEnvelope command = createCommand();
		final StubEventStream stubEventStream = new StubEventStream();
		when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

		when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
		when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
		when(caseProgressionDetails.sendCaseToCrownCourt(CASE_PROGRESSION_ID, CASE_ID)).thenReturn(events);
		when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

		progressionCommandHandler.sendToCrownCourt(command);

		assertThat(stubEventStream.events, notNullValue());
		assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

	}
	
	@Test
	public void shouldHandlerAddCaseToCrownCourtCommand() throws Exception {
		final JsonEnvelope command = createAddCaseCommand();
		final StubEventStream stubEventStream = new StubEventStream();
		when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

		when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
		when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
		when(caseProgressionDetails.addCaseToCrownCourt(CASE_PROGRESSION_ID, CASE_ID,COURT_CENTRE_ID)).thenReturn(events);
		when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

		progressionCommandHandler.addCaseToCrownCourt(command);

		assertThat(stubEventStream.events, notNullValue());
		assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

	}


	@Test
	public void shouldHandlerAddDefenceIssuesCommand() throws Exception {
		final JsonEnvelope command = createAddDefenceIssuesCommand();
		final StubEventStream stubEventStream = new StubEventStream();
		when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

		when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
		when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
		when(caseProgressionDetails.addDefenceIssues(CASE_PROGRESSION_ID, DEFENCE_ISSUE, 1L)).thenReturn(events);
		when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

		progressionCommandHandler.addDefenceIssues(command);

		assertThat(stubEventStream.events, notNullValue());
		assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

	}
	
	@Test
	public void shouldHandlerAddSfrIssuesCommand() throws Exception {
		final JsonEnvelope command = createAddSfrIssuesCommand();
		final StubEventStream stubEventStream = new StubEventStream();
		when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

		when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
		when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
		when(caseProgressionDetails.addSfrIssues(CASE_PROGRESSION_ID, SFR_ISSUE, 1L)).thenReturn(events);
		when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

		progressionCommandHandler.addSfrIssues(command);

		assertThat(stubEventStream.events, notNullValue());
		assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

	}
	
	@Test
    public void shouldHandlerSendCommittalHearingInformationCommand() throws Exception {
        final JsonEnvelope command = createSendCommittalHearingInformationCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.sendCommittalHearingInformation(CASE_PROGRESSION_ID, FROM_COURT_CENTRE, SENDING_COMMITTAL_DATE, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.sendCommittalHearingInformation(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

    }
	
	@Test
    public void shouldAddDefenceTrialEstimate() throws Exception {
        final JsonEnvelope command = createDefenceTrialEstimateCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.addDefenceTrialEstimate(CASE_PROGRESSION_ID, DEFENCE_TRIAL_ESTIMATE, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.addDefenceTrialEstimate(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));

    }
	
    @Test
    public void shouldAddProsecutionTrialEstimate() throws Exception {
        final JsonEnvelope command = createProsecutionTrialEstimateCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.addProsecutionTrialEstimate(CASE_PROGRESSION_ID,PROSECUTION_TRIAL_ESTIMATE, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.addProsecutionTrialEstimate(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }
    
	@Test
    public void shouldIssueDirection() throws Exception {
	    final JsonEnvelope command = createIssueDirectionCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.issueDirection(CASE_PROGRESSION_ID, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.issueDirection(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }
	
	@Test
	public void shouldPreSentenceReport() throws Exception {
	    final JsonEnvelope command = createPreSentenceReportCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.preSentenceReport(CASE_PROGRESSION_ID,IS_PSR_ORDERED, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.preSentenceReport(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }

	@Test
    public void shouldIndicatestatement() throws Exception {
	    final JsonEnvelope command = createIndicatestatementCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(INDICATE_STATEMENT_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, IndicateStatement.class)).thenReturn(indicateStatement);
        when(indicateStatement.serveIndicateEvidence(INDICATE_STATEMENT_ID, CASE_ID, PLAN_DATE, EVIDENCE_NAME, true)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.indicatestatement(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
    }
	
	@Test
	public void shouldIndicateAllStatementsIdentified() throws Exception {
	    final JsonEnvelope command = createIndicateAllStatementsIdentifiedCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.allStatementsIdentified(CASE_PROGRESSION_ID, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.indicateAllStatementsIdentified(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
	}	
	
	@Test
	public void shouldIndicateAllStatementsServed() throws Exception {
        final JsonEnvelope command = createIndicateAllStatementsServedCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.allStatementsServed(CASE_PROGRESSION_ID, 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.indicateAllStatementsServed(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
	}
	 
	@Test
	public void shouldVacatePTPHearing() throws Exception {
	    final JsonEnvelope command = createVacatePTPHearingCommand();
        final StubEventStream stubEventStream = new StubEventStream();
        when(enveloper.withMetadataFrom(command)).thenReturn(enveloperFunction);

        when(eventSource.getStreamById(CASE_PROGRESSION_ID)).thenReturn(stubEventStream);
        when(aggregateService.get(stubEventStream, CaseProgressionDetails.class)).thenReturn(caseProgressionDetails);
        when(caseProgressionDetails.vacatePTPHearing(CASE_PROGRESSION_ID,LocalDate.now(), 1L)).thenReturn(events);
        when(events.map(enveloperFunction)).thenReturn(Arrays.asList(envelope).stream());

        progressionCommandHandler.vacatePTPHearing(command);

        assertThat(stubEventStream.events, notNullValue());
        assertThat(stubEventStream.events.findFirst().get(), equalTo(envelope));
	}
	 
    private JsonEnvelope createVacatePTPHearingCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, ALL_STATEMENT_SERVED_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }

    private JsonEnvelope createIndicateAllStatementsServedCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, ALL_STATEMENT_SERVED_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }

    private JsonEnvelope createIndicateAllStatementsIdentifiedCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, ALL_STATEMENT_IDENTIFIED_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }

    private JsonEnvelope createIndicatestatementCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, PRE_SENTENCE_REPORT_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("indicateStatementId", INDICATE_STATEMENT_ID.toString())
                .add("isKeyEvidence", IS_KEY_EVIDENCE)
                .add("planDate", PLAN_DATE.toString())
                .add("caseId", CASE_ID.toString())
                .add("evidenceName", EVIDENCE_NAME).build();
        

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }

    private JsonEnvelope createPreSentenceReportCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, PRE_SENTENCE_REPORT_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("isPSROrdered", IS_PSR_ORDERED)
                .add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }

    private JsonEnvelope createSendCommittalHearingInformationCommand() {
	    final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, SENDING_COMMITTAL_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("caseId", CASE_ID.toString()).add("fromCourtCentre", FROM_COURT_CENTRE.toString())
                .add("sendingCommittalDate", SENDING_COMMITTAL_DATE.toString()).add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }

    private JsonEnvelope createAddDefenceIssuesCommand() {
		final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
				.add(NAME, DEFENCE_ISSUE_EVENT_NAME).build();

		final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
				.add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("defenceIssues", DEFENCE_ISSUE).add("version", VERSION).build();

		return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
				payloadAsJsonObject);
	}
	
	private JsonEnvelope createAddSfrIssuesCommand() {
		final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
				.add(NAME, SFR_ISSUE_EVENT_NAME).build();

		final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
				.add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("sfrIssues", SFR_ISSUE).add("version", VERSION).build();

		return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
				payloadAsJsonObject);
	}

	private JsonEnvelope createCommand() {
		final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
				.add(NAME, EVENT_NAME).build();

		final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
				.add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("caseId", CASE_ID.toString()).build();

		return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
				payloadAsJsonObject);

	}
	
	private JsonEnvelope createAddCaseCommand() {
		final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
				.add(NAME, ADD_CASE_EVENT_NAME).build();

		final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
				.add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("caseId", CASE_ID.toString()).add("courtCentreId", COURT_CENTRE_ID.toString()).build();

		return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
				payloadAsJsonObject);

	}

	
	private JsonEnvelope createDefenceTrialEstimateCommand() {
		final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
				.add(NAME, DEFENCE_TRIAL_ESTIMATE_EVENT_NAME).build();

		final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
				.add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("defenceTrialEstimate", DEFENCE_TRIAL_ESTIMATE).add("version", VERSION).build();

		return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
				payloadAsJsonObject);
	}
	
	private JsonEnvelope createProsecutionTrialEstimateCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, PROSECUTION_TRIAL_ESTIMATE_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("prosecutionTrialEstimate", PROSECUTION_TRIAL_ESTIMATE).add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
    }
	
	private JsonEnvelope createIssueDirectionCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, UUID.randomUUID().toString())
                .add(NAME, DIRECTION_ISSUED_EVENT_NAME).build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder()
                .add("caseProgressionId", CASE_PROGRESSION_ID.toString()).add("version", VERSION).build();

        return DefaultJsonEnvelope.envelopeFrom(JsonObjectMetadata.metadataFrom(metadataAsJsonObject),
                payloadAsJsonObject);
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