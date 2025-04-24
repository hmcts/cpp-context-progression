package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.ShareAllCourtDocuments;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import javax.inject.Inject;
import javax.json.JsonValue;

import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.fromString;
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
        LOGGER.debug("progression.command.share-court-document {}", shareCourtDocumentEnvelope);

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

    @Handles("progression.command.share-all-court-documents")
    public void handleAllCourtDocuments(final Envelope<ShareAllCourtDocuments> shareAllCourtDocumentsEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.command.share-all-court-documents {}", shareAllCourtDocumentsEnvelope);
        }

        final UUID sharedByUser = fromString(shareAllCourtDocumentsEnvelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final ShareAllCourtDocuments shareAllCourtDocuments = shareAllCourtDocumentsEnvelope.payload();

        final UUID caseId = shareAllCourtDocuments.getCaseId();
        final UUID defendantId = shareAllCourtDocuments.getDefendantId();
        final UUID applicationHearingId = shareAllCourtDocuments.getApplicationHearingId();
        final UUID userGroupId = shareAllCourtDocuments.getUserGroupId();
        final UUID userId = shareAllCourtDocuments.getUserId();

        final EventStream eventStream = eventSource.getStreamById(applicationHearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = hearingAggregate.shareAllCourtDocuments(applicationHearingId, caseId, defendantId, userGroupId, userId, sharedByUser);
        appendEventsToStream(shareAllCourtDocumentsEnvelope, eventStream, events);
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
