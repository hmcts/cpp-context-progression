package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.RecordNowsDocumentFailed;
import uk.gov.justice.core.courts.RecordNowsDocumentSent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.progression.courts.RecordNowsDocumentGenerated;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class NowDocumentRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NowDocumentRequestHandler.class.getName());

    private static final String PROGRESSION_COMMAND_REQUEST_NOW_DOCUMENT = "progression.command.add-now-document-request";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(PROGRESSION_COMMAND_REQUEST_NOW_DOCUMENT)
    public void handleAddNowDocumentRequest(final Envelope<NowDocumentRequest> envelope) throws EventStreamException {
        final NowDocumentRequest nowDocumentRequest = envelope.payload();
        final UUID materialId = nowDocumentRequest.getMaterialId();
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final EventStream eventStream = eventSource.getStreamById(nowDocumentRequest.getMaterialId());

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.createNowDocumentRequest(materialId, nowDocumentRequest, userId);

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.record-nows-document-sent")
    public void recordNowsDocumentSent(final Envelope<RecordNowsDocumentSent> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-nows-document-sent {}", envelope.payload());

        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final RecordNowsDocumentSent recordNowsDocumentSent = envelope.payload();

        final UUID materialId = recordNowsDocumentSent.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowsDocumentSent(materialId, userId, recordNowsDocumentSent);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.record-nows-document-generated")
    public void recordNowsDocumentGenerated(final Envelope<RecordNowsDocumentGenerated> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-nows-document-generated {}", envelope.payload());

        final RecordNowsDocumentGenerated recordNowsDocumentGenerated = envelope.payload();

        final UUID materialId = recordNowsDocumentGenerated.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowsDocumentGenerated(materialId, recordNowsDocumentGenerated);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.record-nows-document-failed")
    public void recordNowsDocumentFailed(final Envelope<RecordNowsDocumentFailed> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-nows-document-failed {}", envelope.payload());

        final RecordNowsDocumentFailed recordNowsDocumentFailed = envelope.payload();

        final UUID materialId = recordNowsDocumentFailed.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);

        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

        final Stream<Object> events = materialAggregate.recordNowsDocumentFailed(materialId, recordNowsDocumentFailed);

        appendEventsToStream(envelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}

