package uk.gov.moj.cpp.progression.handler;

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
import uk.gov.moj.cpp.progression.command.UpdateCaseStatusBdf;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStatusHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-case-status-bdf")
    public void handleUpdateCaseStatusBdf(final Envelope<UpdateCaseStatusBdf> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-case-status-bdf {}", envelope);

        final UpdateCaseStatusBdf updateCaseStatusBdf = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(UUID.randomUUID());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = caseAggregate.updateCaseStatusBdf(updateCaseStatusBdf.getProsecutionCaseId(), updateCaseStatusBdf.getCaseStatus(), updateCaseStatusBdf.getNotes());

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);

        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
