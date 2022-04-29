package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddConvictionDate;
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
public class AddConvictionDateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddConvictionDateHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.add-conviction-date")
    public void handle(final Envelope<AddConvictionDate> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.add-conviction-date {}", envelope);
        final AddConvictionDate addConvictionDate = envelope.payload();

        final EventStream eventStream;
        final Stream<Object> events;
        if(addConvictionDate.getCourtApplicationId() == null) {
            eventStream = eventSource.getStreamById(addConvictionDate.getCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            events = caseAggregate.addConvictionDate(addConvictionDate.getCaseId(), addConvictionDate.getOffenceId(), addConvictionDate.getConvictionDate());
        }else{
            eventStream = eventSource.getStreamById(addConvictionDate.getCourtApplicationId());
            final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
            events = applicationAggregate.addConvictionDate(addConvictionDate.getCourtApplicationId(), addConvictionDate.getOffenceId(), addConvictionDate.getConvictionDate());
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
