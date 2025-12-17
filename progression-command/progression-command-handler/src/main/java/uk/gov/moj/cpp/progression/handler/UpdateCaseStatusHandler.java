package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.HearingConfirmedUpdateCaseStatus;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCaseStatusHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCaseStatusHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.hearing-confirmed-update-case-status")
    public void handleUpdateCaseStatus(final Envelope<HearingConfirmedUpdateCaseStatus> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.hearing-confirmed-update-case-status {}", envelope.payload());
        final HearingConfirmedUpdateCaseStatus payload = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(payload.getProsecutionCase().getId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateCaseStatus(payload.getProsecutionCase(), payload.getCaseStatus());
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}