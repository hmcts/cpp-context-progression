package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.RemoveCourtDocument;
import uk.gov.justice.core.courts.RemoveCourtDocumentBdf;
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
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class RemoveCourtDocumentHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveCourtDocumentHandler.class.getName());
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;


    @Handles("progression.command.remove-court-document")
    public void handle(final Envelope<RemoveCourtDocument> removeCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.remove-court-document {}", removeCourtDocumentEnvelope.payload());
        final UUID courtDocumentId = removeCourtDocumentEnvelope.payload().getCourtDocumentId();
        final UUID materialId = removeCourtDocumentEnvelope.payload().getMaterialId();
        final boolean isRemoved = removeCourtDocumentEnvelope.payload().getIsRemoved();
        final EventStream eventStream = eventSource.getStreamById(courtDocumentId);
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);
        final Stream<Object> events = courtDocumentAggregate.removeCourtDocument(courtDocumentId,materialId,isRemoved);
        appendEventsToStream(removeCourtDocumentEnvelope, eventStream, events);
    }

    @Handles("progression.command.remove-court-document-bdf")
    public void handleBdf(final Envelope<RemoveCourtDocumentBdf> removeCourtDocumentBdfEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.remove-court-document-bdf {}", removeCourtDocumentBdfEnvelope.payload());
        final UUID courtDocumentId = removeCourtDocumentBdfEnvelope.payload().getCourtDocumentId();
        final boolean isRemoved = removeCourtDocumentBdfEnvelope.payload().getIsRemoved();
        final EventStream eventStream = eventSource.getStreamById(courtDocumentId);
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);
        final Stream<Object> events = courtDocumentAggregate.removeCourtDocumentByBdf(courtDocumentId,isRemoved);
        appendEventsToStreamBdf(removeCourtDocumentBdfEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<RemoveCourtDocument> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

    private void appendEventsToStreamBdf(final Envelope<RemoveCourtDocumentBdf> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
