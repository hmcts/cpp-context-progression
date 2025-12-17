package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentUpdatedProcessor {

    protected static final String PUBLIC_COURT_DOCUMENT_UPDATED = "public.progression.events.court-document-updated";

    @Inject
    private Sender sender;

    @Handles("progression.event.court-document-updated")
    public void handleCourtDocumentUpdatedEvent(final JsonEnvelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_UPDATED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }
}
