package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;

import uk.gov.justice.core.courts.CourtsDocumentUploaded;
import uk.gov.justice.core.courts.UploadCourtDocument;
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

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class UploadCourtDocumentHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;


    @Handles("progression.command.upload-court-document")
    public void handle(final Envelope<UploadCourtDocument> courtDocumentEnvelope) throws EventStreamException {
        final UUID fileServiceId = courtDocumentEnvelope.payload().getFileServiceId();
        final UUID materialId = courtDocumentEnvelope.payload().getMaterialId();
        final EventStream eventStream = eventSource.getStreamById(randomUUID());
        appendEventsToStream(courtDocumentEnvelope, eventStream, Stream.of(CourtsDocumentUploaded.courtsDocumentUploaded().withMaterialId(materialId).withFileServiceId(fileServiceId).build()));

    }
    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }


}
