package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.RemoveConvictionDate;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RemoveConvictionDateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveConvictionDateHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.remove-conviction-date")
    public void handle(final Envelope<RemoveConvictionDate> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.remove-conviction-date {}", envelope);
        final RemoveConvictionDate removeConvictionDate = envelope.payload();
        final EventStream eventStream;
        final Stream<Object> events;

        if(removeConvictionDate.getCourtApplicationId() == null) {
            eventStream = eventSource.getStreamById(removeConvictionDate.getCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            events = caseAggregate.removeConvictionDate(removeConvictionDate.getCaseId(), removeConvictionDate.getOffenceId());
        }else{
            eventStream = eventSource.getStreamById(removeConvictionDate.getCourtApplicationId());
            final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
            events = applicationAggregate.removeConvictionDate(removeConvictionDate.getCourtApplicationId(), removeConvictionDate.getOffenceId());
        }
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
