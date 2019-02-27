package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.CreateProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCase;
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

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;


@SuppressWarnings("squid:S2629")
@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateProsecutionCaseHandler {


    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateProsecutionCaseHandler.class.getName());
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.create-prosecution-case")
    public void handle(final Envelope<CreateProsecutionCase> createProsecutionCaseEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.create-prosecution-case {}", createProsecutionCaseEnvelope);
        final ProsecutionCase prosecutionCase = createProsecutionCaseEnvelope.payload().getProsecutionCase();
        final EventStream eventStream = eventSource.getStreamById(prosecutionCase.getId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.createProsecutionCase(prosecutionCase);
        appendEventsToStream(createProsecutionCaseEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }


}
