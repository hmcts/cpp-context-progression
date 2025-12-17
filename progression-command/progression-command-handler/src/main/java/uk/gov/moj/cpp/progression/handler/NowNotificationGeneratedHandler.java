package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.progression.helper.EventStreamHelper.appendEventsToStream;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.command.RecordNowNotificationGenerated;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NowNotificationGeneratedHandler {

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.record-now-notification-generated")
    public void recordNowNotificationGenerated(final Envelope<RecordNowNotificationGenerated> envelope) throws EventStreamException {
        final RecordNowNotificationGenerated recordNowNotificationGenerated = envelope.payload();
        final UUID materialId = recordNowNotificationGenerated.getMaterialId();
        final UUID hearingId = recordNowNotificationGenerated.getHearingId();
        final String status = recordNowNotificationGenerated.getStatus();

        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from progression.command.record-now-notification-generated envelope.")));

        final EventStream eventStream = eventSource.getStreamById(recordNowNotificationGenerated.getMaterialId());

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowNotificationGenerated(materialId, hearingId, status, userId);

        appendEventsToStream(envelope, eventStream, events);
    }
}
