package uk.gov.moj.cpp.progression.command.handler;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.CaseProgressionDetails;
import uk.gov.moj.cpp.progression.domain.IndicateStatement;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ProgressionCommandHandler {

	private static final String FIELD_CASE_PROGRESSION_ID = "caseProgressionId";
	private static final String FIELD_CASE_ID = "caseId";
	private static final String FIELD_DEFENCE_ISSUES = "defenceIssues";
	private static final String FIELD_SFR_ISSUES = "sfrIssues";
	private static final String FIELD_VERSION = "version";
	private static final String FIELD_COURT_CENTER_ID_ = "courtCentreId";
	private static final String FIELD_FROM_COURT_CENTRE = "fromCourtCentre";
	private static final String FIELD_SENDING_COMMITTAL_DATE = "sendingCommittalDate";
	private static final String FIELD_DEFENCE_TRIAL_ESTIMATE = "defenceTrialEstimate";
	private static final String FIELD_PROSECUTION_TRIAL_ESTIMATE = "prosecutionTrialEstimate";
	private static final String FIELD_IS_PSR_ORDERED = "isPSROrdered";
	private static final String FIELD_INDICATE_STATEMENT_ID = "indicateStatementId";
	private static final String FIELD_IS_KEY_EVIDENCE = "isKeyEvidence";
	private static final String FIELD_EVIDENCE_NAME = "evidenceName";
	private static final String FIELD_PLAN_DATE = "planDate";
	
	@Inject
	EventSource eventSource;

	@Inject
	Enveloper enveloper;

	@Inject
	AggregateService aggregateService;


	@Handles("progression.command.send-to-crown-court")
	public void sendToCrownCourt(final JsonEnvelope envelope) throws EventStreamException {
		final UUID caseProgressionId = UUID
				.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
		final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));

		EventStream eventStream = eventSource.getStreamById(caseProgressionId);
		CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
		eventStream.append(caseProgressionDetails.sendCaseToCrownCourt(caseProgressionId, caseId)
				.map(enveloper.withMetadataFrom(envelope)));

	}
	
	@Handles("progression.command.add-case-to-crown-court")
	public void addCaseToCrownCourt(final JsonEnvelope envelope)  throws EventStreamException {
		final UUID caseProgressionId = UUID
				.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
		final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
		final String courtCentreId = envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTER_ID_);
		EventStream eventStream = eventSource.getStreamById(caseProgressionId);
		CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
		eventStream.append(caseProgressionDetails.addCaseToCrownCourt(caseProgressionId, caseId,courtCentreId)
				.map(enveloper.withMetadataFrom(envelope)));
	}
	
	@Handles("progression.command.add-defence-issues")
	public void addDefenceIssues(final JsonEnvelope envelope) throws EventStreamException {
		final UUID caseProgressionId = UUID
				.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
		final String defenceIssues =envelope.payloadAsJsonObject().getString(FIELD_DEFENCE_ISSUES);
		final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
		EventStream eventStream = eventSource.getStreamById(caseProgressionId);
		CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
		eventStream.appendAfter(caseProgressionDetails.addDefenceIssues(caseProgressionId, defenceIssues, version)
				.map(enveloper.withMetadataFrom(envelope)),version);

	}
	
	@Handles("progression.command.addsfrissues")
	public void addSfrIssues(final JsonEnvelope envelope) throws EventStreamException{
		final UUID caseProgressionId = UUID
				.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
		final String sfrIssues = envelope.payloadAsJsonObject().getString(FIELD_SFR_ISSUES);
		final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
		EventStream eventStream = eventSource.getStreamById(caseProgressionId);
		CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
		eventStream.appendAfter(caseProgressionDetails.addSfrIssues(caseProgressionId, sfrIssues, version)
				.map(enveloper.withMetadataFrom(envelope)),version);

	}
	
	@Handles("progression.command.sending-committal-hearing-information")
	public void sendCommittalHearingInformation(final JsonEnvelope envelope) throws EventStreamException {
		final UUID caseProgressionId = UUID
				.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
		final String fromCourtCentre = envelope.payloadAsJsonObject().getString(FIELD_FROM_COURT_CENTRE);
		final LocalDate sendingCommittalDate =  LocalDate.parse(envelope.payloadAsJsonObject().getString(FIELD_SENDING_COMMITTAL_DATE));
		final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
		EventStream eventStream = eventSource.getStreamById(caseProgressionId);
		CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
		eventStream.appendAfter(caseProgressionDetails.sendCommittalHearingInformation(caseProgressionId, fromCourtCentre, sendingCommittalDate, version)
				.map(enveloper.withMetadataFrom(envelope)),version);
	}
	
	@Handles("progression.command.defence-trial-estimate")
	public void addDefenceTrialEstimate(final JsonEnvelope envelope) throws EventStreamException{
		final UUID caseProgressionId = UUID
				.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
		final int defenceTrialEstimate = envelope.payloadAsJsonObject().getInt(FIELD_DEFENCE_TRIAL_ESTIMATE);
		final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
		EventStream eventStream = eventSource.getStreamById(caseProgressionId);
		CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
		eventStream.appendAfter(caseProgressionDetails.addDefenceTrialEstimate(caseProgressionId, defenceTrialEstimate, version)
				.map(enveloper.withMetadataFrom(envelope)),version);
	}
	
	@Handles("progression.command.prosecution-trial-estimate")
    public void addProsecutionTrialEstimate(final JsonEnvelope envelope) throws EventStreamException {
	    final UUID caseProgressionId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final int prosecutionTrialEstimate = envelope.payloadAsJsonObject().getInt(FIELD_PROSECUTION_TRIAL_ESTIMATE);
        final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
        EventStream eventStream = eventSource.getStreamById(caseProgressionId);
        CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
        eventStream.appendAfter(caseProgressionDetails.addProsecutionTrialEstimate(caseProgressionId, prosecutionTrialEstimate, version)
                .map(enveloper.withMetadataFrom(envelope)),version);
    }
	
	@Handles("progression.command.issue-direction")
    public void issueDirection(final JsonEnvelope envelope)  throws EventStreamException {
	    final UUID caseProgressionId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
	    final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
        EventStream eventStream = eventSource.getStreamById(caseProgressionId);
        CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
        eventStream.appendAfter(caseProgressionDetails.issueDirection(caseProgressionId,  version)
                .map(enveloper.withMetadataFrom(envelope)),version);
    }
	
	@Handles("progression.command.pre-sentence-report")
    public void preSentenceReport(final JsonEnvelope envelope) throws EventStreamException {
	    final UUID caseProgressionId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
	    final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
        final Boolean isPSROrdered = envelope.payloadAsJsonObject().getBoolean(FIELD_IS_PSR_ORDERED);
        EventStream eventStream = eventSource.getStreamById(caseProgressionId);
        CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
        eventStream.appendAfter(caseProgressionDetails.preSentenceReport(caseProgressionId,isPSROrdered, version)
                .map(enveloper.withMetadataFrom(envelope)),version);
    }
	
	@Handles("progression.command.indicate-statement")
    public void indicatestatement(final JsonEnvelope envelope) throws EventStreamException {
	    final UUID indicateStatementId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_INDICATE_STATEMENT_ID));
	    final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String evidenceName = envelope.payloadAsJsonObject().getString(FIELD_EVIDENCE_NAME);
        final Boolean isKeyEvidence = envelope.payloadAsJsonObject().getBoolean(FIELD_IS_KEY_EVIDENCE);
        final LocalDate planDate = LocalDate.parse(envelope.payloadAsJsonObject().getString(FIELD_PLAN_DATE));
        EventStream eventStream = eventSource.getStreamById(indicateStatementId);
        IndicateStatement indicateStatement = aggregateService.get(eventStream, IndicateStatement.class);
        eventStream.append(indicateStatement.serveIndicateEvidence(indicateStatementId, caseId, planDate, evidenceName, isKeyEvidence)
                .map(enveloper.withMetadataFrom(envelope)));
        
    }
	
	@Handles("progression.command.indicate-all-statements-identified")
    public void indicateAllStatementsIdentified(final JsonEnvelope envelope) throws EventStreamException {
	    final UUID caseProgressionId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
	    final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
        EventStream eventStream = eventSource.getStreamById(caseProgressionId);
        CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
        eventStream.appendAfter(caseProgressionDetails.allStatementsIdentified(caseProgressionId, version)
                .map(enveloper.withMetadataFrom(envelope)),version);
    }
	
	@Handles("progression.command.indicate-all-statements-served")
    public void indicateAllStatementsServed(final JsonEnvelope envelope) throws EventStreamException {
	    final UUID caseProgressionId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
	    final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
        EventStream eventStream = eventSource.getStreamById(caseProgressionId);
        CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
        eventStream.appendAfter(caseProgressionDetails.allStatementsServed(caseProgressionId, version)
                .map(enveloper.withMetadataFrom(envelope)),version);
    }
	
	@Handles("progression.command.vacate-ptp-hearing")
    public void vacatePTPHearing(final JsonEnvelope envelope) throws EventStreamException {
	    final UUID caseProgressionId = UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
	    final Long version = new Long(envelope.payloadAsJsonObject().getString(FIELD_VERSION));
        EventStream eventStream = eventSource.getStreamById(caseProgressionId);
        CaseProgressionDetails caseProgressionDetails = aggregateService.get(eventStream, CaseProgressionDetails.class);
        eventStream.appendAfter(caseProgressionDetails.vacatePTPHearing(caseProgressionId, LocalDate.now(),version)
                .map(enveloper.withMetadataFrom(envelope)),version);
    }
}
