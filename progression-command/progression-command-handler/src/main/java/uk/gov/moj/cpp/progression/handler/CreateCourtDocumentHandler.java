package uk.gov.moj.cpp.progression.handler;

import static uk.gov.moj.cpp.progression.helper.CourtDocumentHelper.setDefaults;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CreateCourtDocument;
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


@SuppressWarnings({"squid:S3655", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateCourtDocumentHandler {


    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateCourtDocumentHandler.class.getName());


    @Handles("progression.command.create-court-document")
    public void handle(final Envelope<CreateCourtDocument> createCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.create-court-document {}", createCourtDocumentEnvelope);
        final CourtDocument courtDocument = setDefaults(createCourtDocumentEnvelope.payload().getCourtDocument());
        final EventStream eventStream = eventSource.getStreamById(courtDocument.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);
        final Stream<Object> events = courtDocumentAggregate.createCourtDocument(courtDocument);
        appendEventsToStream(createCourtDocumentEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
