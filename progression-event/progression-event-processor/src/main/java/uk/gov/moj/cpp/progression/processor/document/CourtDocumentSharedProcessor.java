package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;


@SuppressWarnings({"squid:S4144"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentSharedProcessor {

    public static final String PUBLIC_COURT_DOCUMENT_SHARED = "public.progression.event.court-document-shared";
    public static final String PUBLIC_ALL_COURT_DOCUMENTS_SHARED = "public.progression.event.all-court-documents-shared";

    @Inject
    private Sender sender;

    @Handles("progression.event.court-document-shared")
    public void handleCourtDocumentSharedEvent(final JsonEnvelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_SHARED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }

    @Handles("progression.event.court-document-shared-v2")
    public void handleCourtDocumentSharedEventV2(final JsonEnvelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_SHARED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }

    @Handles("progression.event.duplicate-share-court-document-request-received")
    public void handleDuplicateShareCourtDocumentRequestReceivedEvent(final JsonEnvelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_SHARED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }

    @Handles("progression.event.all-court-documents-shared")
    public void handleAllCourtDocumentsSharedEvent(final JsonEnvelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_ALL_COURT_DOCUMENTS_SHARED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }

    @Handles("progression.event.duplicate-share-all-court-documents-request-received")
    public void handleDuplicateAllShareCourtDocumentsRequestReceivedEvent(final JsonEnvelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_ALL_COURT_DOCUMENTS_SHARED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }

}
