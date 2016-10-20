package uk.gov.moj.cpp.progression.command.handler;

import static uk.gov.justice.services.eventsourcing.source.core.Events.streamOf;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ProgressionCommandHandler {

	public static final String FIELD_CASE_PROGRESSION_ID = "caseProgressionId";

	@Inject
	EventSource eventSource;

	@Inject
	Enveloper enveloper;

	@Inject
	ProgressionEventFactory progressionEventFactory;

	@Inject
	JsonObjectToObjectConverter jsonObjectToObjectConverter;

	@Handles("progression.command.add-case-to-progression")
	public void addCaseToCrownCourt(final JsonEnvelope envelope) throws EventStreamException {
		Stream<Object> caseAddedToCrownCourt = streamOf(progressionEventFactory.createCaseAddedToCrownCourt(envelope));
		EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
		eventStream.append(caseAddedToCrownCourt.map(enveloper.withMetadataFrom(envelope)));
	}

	@Handles("progression.command.sending-committal-hearing-information")
	public void sendCommittalHearingInformation(final JsonEnvelope envelope) throws EventStreamException {
		Stream<Object> events = streamOf(
				progressionEventFactory.createSendingCommittalHearingInformationAdded(envelope));
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
		Stream<Object> events = streamOf(progressionEventFactory.createPreSentenceReportOrdered(envelope));
		EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
		eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
	}

	@Handles("progression.command.sentence-hearing-date")
	public void addSentenceHearingDate(final JsonEnvelope envelope) throws EventStreamException {
		Stream<Object> events = streamOf(progressionEventFactory.createSentenceHearingDateAdded(envelope));
		EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
		eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
	}

	@Handles("progression.command.case-to-be-assigned")
	public void updateCaseToBeAssigned(final JsonEnvelope envelope) throws EventStreamException {
		Stream<Object> events = streamOf(progressionEventFactory.createCaseToBeAssignedUpdated(envelope));
		EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
		eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
	}

	@Handles("progression.command.case-assigned-for-review")
	public void updateCaseAssignedForReview(final JsonEnvelope envelope) throws EventStreamException {
		Stream<Object> events = streamOf(progressionEventFactory.createCaseAssignedForReviewUpdated(envelope));
		EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
		eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
	}

	@Handles("progression.command.prepare-for-sentence-hearing")
	public void prepareForSentenceHearing(final JsonEnvelope envelope) throws EventStreamException {
		Stream<Object> events = streamOf(progressionEventFactory.createCaseReadyForSentenceHearing(envelope));
		EventStream eventStream = eventSource.getStreamById(getCaseProgressionId(envelope));
		eventStream.append(events.map(enveloper.withMetadataFrom(envelope)));
	}

	private UUID getCaseProgressionId(final JsonEnvelope envelope) {
		return UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
	}
}
