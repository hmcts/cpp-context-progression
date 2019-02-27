package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.AddCourtDocument;
import uk.gov.justice.core.courts.CourtDocument;
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

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddCourtDocumentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddCourtDocumentHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.add-court-document")
    public void handle(final Envelope<AddCourtDocument>  addCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.add-court-document {}", addCourtDocumentEnvelope );
        final CourtDocument courtDocument = addCourtDocumentEnvelope.payload().getCourtDocument();
        final EventStream eventStream = eventSource.getStreamById(courtDocument.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);
        final Stream<Object> events = courtDocumentAggregate.addCourtDocument(courtDocument);
        appendEventsToStream(addCourtDocumentEnvelope, eventStream, events);
    }
    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
