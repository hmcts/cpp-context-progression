package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.UpdateCourtApplicationStatus;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCourtApplicationStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCourtApplicationStatusHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.update-court-application-status")
    public void handle(final Envelope<UpdateCourtApplicationStatus> updateCourtApplicationStatusEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-court-application-status {}", updateCourtApplicationStatusEnvelope.payload());

        final UpdateCourtApplicationStatus updateCourtApplicationStatus = updateCourtApplicationStatusEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateCourtApplicationStatus.getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.updateApplicationStatus(updateCourtApplicationStatus.getId(), updateCourtApplicationStatus.getApplicationStatus());
        appendEventsToStream(updateCourtApplicationStatusEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
