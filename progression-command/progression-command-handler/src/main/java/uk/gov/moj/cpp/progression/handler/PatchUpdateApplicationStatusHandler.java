package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.isNull;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.PatchUpdateApplicationStatus;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

@SuppressWarnings("squid:S1160")
@ServiceComponent(Component.COMMAND_HANDLER)
public class PatchUpdateApplicationStatusHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.patch-update-application-status")
    public void handle(final Envelope<PatchUpdateApplicationStatus> envelope) throws EventStreamException {
        final PatchUpdateApplicationStatus payload = envelope.payload();
        final UUID applicationId = payload.getId();
        final ApplicationStatus applicationStatus= isNull(payload.getApplicationStatus()) ? ApplicationStatus.IN_PROGRESS : payload.getApplicationStatus();

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate aggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = aggregate.patchUpdateApplicationStatus(applicationStatus);
        appendEventsToStream(envelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        final Stream<JsonEnvelope> envelopeStream = events.map(toEnvelopeWithMetadataFrom(jsonEnvelope));
        eventStream.append(envelopeStream);
    }

}
