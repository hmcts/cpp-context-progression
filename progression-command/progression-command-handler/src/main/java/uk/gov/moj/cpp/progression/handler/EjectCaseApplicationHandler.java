package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.EjectCaseOrApplication;
import uk.gov.justice.core.courts.EjectCaseViaBdf;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EjectCaseApplicationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EjectCaseApplicationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.eject-case-or-application")
    public void handle(final Envelope<EjectCaseOrApplication> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.eject-case-or-application {}", envelope);
        final EjectCaseOrApplication ejectCaseOrApplication = envelope.payload();
        if(nonNull(ejectCaseOrApplication.getProsecutionCaseId())) {
            final EventStream eventStream = eventSource.getStreamById(ejectCaseOrApplication.getProsecutionCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final Stream<Object> events = caseAggregate.ejectCase(ejectCaseOrApplication.getProsecutionCaseId(), ejectCaseOrApplication.getRemovalReason());
            appendEventsToStream(envelope, eventStream, events);
        }
        else{
            final EventStream eventStream = eventSource.getStreamById(ejectCaseOrApplication.getApplicationId());
            final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
            final Stream<Object> events = applicationAggregate.ejectApplication(ejectCaseOrApplication.getApplicationId(), ejectCaseOrApplication.getRemovalReason());
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    @Handles("progression.command.eject-case-via-bdf")
    public void handleForBdf(final Envelope<EjectCaseViaBdf> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.eject-case-via-bdf {}", envelope);
        final EjectCaseViaBdf ejectCaseViaBdf = envelope.payload();
        if(nonNull(ejectCaseViaBdf.getProsecutionCaseId())) {
            final EventStream eventStream = eventSource.getStreamById(ejectCaseViaBdf.getProsecutionCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final Stream<Object> events = caseAggregate.ejectCaseViaBdf(ejectCaseViaBdf.getProsecutionCaseId(), ejectCaseViaBdf.getRemovalReason());
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
