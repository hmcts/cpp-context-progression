package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.progression.courts.ChangeBoxworkAssignment;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00112", "squid:S2629", "squid:CallToDeprecatedMethod"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class ChangeBoxworkAssigmentHandler {

    public static final String COMMAND_NAME = "progression.command.change-boxwork-assignment";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ChangeBoxworkAssigmentHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles(COMMAND_NAME)
    public void handle(final Envelope<ChangeBoxworkAssignment> envelope) throws EventStreamException {
        LOGGER.debug("{} {}", COMMAND_NAME, envelope.payload());

        final ChangeBoxworkAssignment changeBoxworkAssignment = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(changeBoxworkAssignment.getApplicationId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.changeBoxWorkAssignment(changeBoxworkAssignment.getApplicationId(), changeBoxworkAssignment.getUserId());
        appendEventsToStream(envelope, eventStream, events);
        if (applicationAggregate.getBoxHearingId() != null) {
            final EventStream hearingEventStream = eventSource.getStreamById(applicationAggregate.getBoxHearingId());
            final HearingAggregate hearingAggregate = aggregateService.get(hearingEventStream, HearingAggregate.class);
            if (hearingAggregate.getSavedListingStatusChanged() != null) {
                appendEventsToStream(envelope, hearingEventStream, hearingAggregate.assignBoxworkUser(changeBoxworkAssignment.getUserId()));
            }
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
