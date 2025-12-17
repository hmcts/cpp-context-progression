package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.listing.courts.PublishCourtList;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class PublishCourtListHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourtListHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.publish-court-list")
    public void handlePublishCourtList(final Envelope<PublishCourtList> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.publish-court-list {}", envelope.payload());

        final PublishCourtList publishCourtList = envelope.payload();
        final UUID courtCentreId = publishCourtList.getCourtCentreId();
        final EventStream eventStream = eventSource.getStreamById(courtCentreId);
        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);
        final Stream<Object> events = courtCentreAggregate.publishCourtList(courtCentreId, publishCourtList);

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
