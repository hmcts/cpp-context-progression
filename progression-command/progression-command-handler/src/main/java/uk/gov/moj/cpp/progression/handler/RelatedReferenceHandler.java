package uk.gov.moj.cpp.progression.handler;


import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

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
import uk.gov.moj.cpp.progression.command.AddRelatedReference;
import uk.gov.moj.cpp.progression.command.DeleteRelatedReference;
import uk.gov.moj.cpp.progression.events.RelatedReferenceAdded;
import uk.gov.moj.cpp.progression.events.RelatedReferenceDeleted;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RelatedReferenceHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelatedReferenceHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.add-related-reference")
    public void handleAddRelatedReference(final Envelope<AddRelatedReference> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-related-reference: {}", envelope.payload());
        }
        final AddRelatedReference addRelatedReference = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(addRelatedReference.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final RelatedReferenceAdded relatedReferenceAdded = RelatedReferenceAdded
                .relatedReferenceAdded()
                .withRelatedReference(addRelatedReference.getRelatedReference())
                .withRelatedReferenceId(UUID.randomUUID())
                .withProsecutionCaseId(addRelatedReference.getProsecutionCaseId())
                .build();

        final Stream<Object> events = caseAggregate.addRelatedReference(relatedReferenceAdded);

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.delete-related-reference")
    public void handleDeleteRelatedReference(final Envelope<DeleteRelatedReference> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-related-reference payload: {}", envelope.payload());
        }

        final DeleteRelatedReference deleteRelatedReference = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(deleteRelatedReference.getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final RelatedReferenceDeleted relatedReferenceDeleted = RelatedReferenceDeleted
                .relatedReferenceDeleted()
                .withRelatedReferenceId(deleteRelatedReference.getRelatedReferenceId())
                .withProsecutionCaseId(deleteRelatedReference.getProsecutionCaseId())
                .build();

        final Stream<Object> events = caseAggregate.deleteRelatedReference(relatedReferenceDeleted);

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
