package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.InitiateCourtProceedings; 
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CasesReferredToCourtAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(COMMAND_HANDLER)
public class InitiateCourtProceedingsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateCourtProceedingsHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.initiate-court-proceedings")
    public void handle(final Envelope<InitiateCourtProceedings> initiateCourtProceedingsEnvelope) throws EventStreamException{

        LOGGER.info("progression.command.initiate-court-proceedings {}", initiateCourtProceedingsEnvelope.payload());

        final InitiateCourtProceedings command = initiateCourtProceedingsEnvelope.payload();
        final EventStream stream = eventSource.getStreamById(randomUUID());
        final CasesReferredToCourtAggregate aggregate = aggregateService.get(stream, CasesReferredToCourtAggregate.class);
        final Stream<Object> events = aggregate.initiateCourtProceedings(command.getInitiateCourtProceedings());
        appendEventsToStream(initiateCourtProceedingsEnvelope, stream, events);
    }
    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
