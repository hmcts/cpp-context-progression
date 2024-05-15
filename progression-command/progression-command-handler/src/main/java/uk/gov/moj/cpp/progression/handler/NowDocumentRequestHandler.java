package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NowDocumentRequestHandler {

    private static final String PROGRESSION_COMMAND_REQUEST_NOW_DOCUMENT = "progression.command.add-now-document-request";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(PROGRESSION_COMMAND_REQUEST_NOW_DOCUMENT)
    public void handleAddNowDocumentRequest(final Envelope<NowDocumentRequest> envelope) throws EventStreamException {
        final NowDocumentRequest nowDocumentRequest = envelope.payload();

        final UUID materialId = nowDocumentRequest.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(nowDocumentRequest.getMaterialId());

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.createNowDocumentRequest(materialId, nowDocumentRequest);

        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}

