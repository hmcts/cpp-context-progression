package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.AuditCourtDocument;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
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

@ServiceComponent(Component.COMMAND_HANDLER)
public class AuditCourtDocumentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditCourtDocumentHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.audit-court-document")
    public void handle(final Envelope<AuditCourtDocument> auditCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.audit-court-document {}", auditCourtDocumentEnvelope);

        final String userId = auditCourtDocumentEnvelope.metadata().userId()
                .orElseThrow(() -> new IllegalArgumentException("No UserId Supplied"));

        final AuditCourtDocument payload = auditCourtDocumentEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(payload.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);

        final Stream<Object> events = courtDocumentAggregate
                            .auditCourtDocument(UUID.fromString(userId),
                                    payload.getAction(), payload.getMaterialId());
        appendEventsToStream(auditCourtDocumentEnvelope, eventStream, events);
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
