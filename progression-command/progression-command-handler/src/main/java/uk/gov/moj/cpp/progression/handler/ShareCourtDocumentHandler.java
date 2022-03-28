package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ShareCourtDocument;
import uk.gov.justice.core.courts.SharedCourtDocument;
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

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;


@SuppressWarnings({"squid:S3655", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class ShareCourtDocumentHandler {


    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ShareCourtDocumentHandler.class.getName());


    @Handles("progression.command.share-court-document")
    public void handle(final Envelope<ShareCourtDocument> shareCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.share-court-document {}", "hearingId: " + shareCourtDocumentEnvelope.payload().getShareCourtDocumentDetails().getHearingId() + " , " + "documentId: " + shareCourtDocumentEnvelope.payload().getShareCourtDocumentDetails().getCourtDocumentId());

        final SharedCourtDocument shareCourtDocumentDetails = shareCourtDocumentEnvelope.payload().getShareCourtDocumentDetails();
        final EventStream eventStream = eventSource.getStreamById(shareCourtDocumentDetails.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(eventStream, CourtDocumentAggregate.class);

        final Stream<Object> events = courtDocumentAggregate
                .shareCourtDocument(shareCourtDocumentDetails.getCourtDocumentId(),
                        shareCourtDocumentDetails.getHearingId(),
                        shareCourtDocumentDetails.getUserGroupId(),
                        shareCourtDocumentDetails.getUserId());
        appendEventsToStream(shareCourtDocumentEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
