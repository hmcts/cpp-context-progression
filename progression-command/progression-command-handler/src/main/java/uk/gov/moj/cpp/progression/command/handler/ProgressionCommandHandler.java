package uk.gov.moj.cpp.progression.command.handler;

import static uk.gov.justice.services.eventsourcing.source.core.Events.streamOf;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ProgressionCommandHandler {

    public static final String FIELD_CASE_PROGRESSION_ID = "caseProgressionId";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_INDICATE_STATEMENT_ID = "indicateStatementId";

    @Inject
    EventSource eventSource;

    @Inject
    Enveloper enveloper;

    @Inject
    ProgressionEventFactory progressionEventFactory;

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.command.send-to-crown-court")
    public void sendToCrownCourt(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> caseSentToCrownCourt =
                        streamOf(progressionEventFactory.createCaseSentToCrownCourt(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append((caseSentToCrownCourt).map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.add-case-to-crown-court")
    public void addCaseToCrownCourt(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> caseAddedToCrownCourt =
                        streamOf(progressionEventFactory.createCaseAddedToCrownCourt(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(caseAddedToCrownCourt.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.add-defence-issues")
    public void addDefenceIssues(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createDefenceIssuesAdded(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.addsfrissues")
    public void addSfrIssues(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events = streamOf(progressionEventFactory.createSfrIssuesAdded(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));

    }

    @Handles("progression.command.sending-committal-hearing-information")
    public void sendCommittalHearingInformation(final JsonEnvelope envelope)
                    throws EventStreamException {
        Stream<Object> events = streamOf(progressionEventFactory
                        .createSendingCommittalHearingInformationAdded(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.defence-trial-estimate")
    public void addDefenceTrialEstimate(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createDefenceTrialEstimateAdded(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.prosecution-trial-estimate")
    public void addProsecutionTrialEstimate(final JsonEnvelope envelope)
                    throws EventStreamException {
        Stream<Object> events = streamOf(
                        progressionEventFactory.createProsecutionTrialEstimateAdded(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.issue-direction")
    public void issueDirection(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events = streamOf(progressionEventFactory.createDirectionIssued(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.pre-sentence-report")
    public void preSentenceReport(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createPreSentenceReportOrdered(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.indicate-statement")
    public void indicatestatement(final JsonEnvelope envelope) throws EventStreamException {
        final UUID indicateStatementId = UUID.fromString(
                        envelope.payloadAsJsonObject().getString(FIELD_INDICATE_STATEMENT_ID));
        Stream<Object> events =
                        streamOf(progressionEventFactory.createIndicateEvidenceServed(envelope));
        EventStream eventStream = eventSource.getStreamById(indicateStatementId);
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));

    }

    @Handles("progression.command.indicate-all-statements-identified")
    public void indicateAllStatementsIdentified(final JsonEnvelope envelope)
                    throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createAllStatementsIdentified(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.indicate-all-statements-served")
    public void indicateAllStatementsServed(final JsonEnvelope envelope)
                    throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createAllStatementsServed(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.vacate-ptp-hearing")
    public void vacatePTPHearing(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events = streamOf(progressionEventFactory.createPTPHearingVacated(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.sentence-hearing-date")
    public void addSentenceHearingDate(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createSentenceHearingDateAdded(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.case-to-be-assigned")
    public void updateCaseToBeAssigned(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events =
                        streamOf(progressionEventFactory.createCaseToBeAssignedUpdated(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.case-assigned-for-review")
    public void updateCaseAssignedForReview(final JsonEnvelope envelope)
                    throws EventStreamException {
        Stream<Object> events = streamOf(
                        progressionEventFactory.createCaseAssignedForReviewUpdated(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }

    @Handles("progression.command.prepare-for-sentence-hearing")
    public void prepareForSentenceHearing(final JsonEnvelope envelope) throws EventStreamException {
        Stream<Object> events = streamOf(
                        progressionEventFactory.createCaseReadyForSentenceHearing(envelope));
        EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
        eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
    }


    @Handles("progression.command.defendant")
    public void addDefendant(final JsonEnvelope envelope) throws EventStreamException {

        final DefendantCommand defendant = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantCommand.class);

        final UUID streamId = defendant.getDefendantProgressionId();

        final Stream<Object> events = streamOf(progressionEventFactory.addDefendantEvent(defendant));
        try {
            EventStream eventStream = eventSource.getStreamById(streamId);
            eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Error while adding event to EventStream", e);
        }
    }

    private Long getVersion(final JsonEnvelope envelope) {
        return new Long(envelope.payloadAsJsonObject().getInt(FIELD_VERSION));
    }

    private UUID getCaseProgressionId(final JsonEnvelope envelope) {
        return UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
    }
}
