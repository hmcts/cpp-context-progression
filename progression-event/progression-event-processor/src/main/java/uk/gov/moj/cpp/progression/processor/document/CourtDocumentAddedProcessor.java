package uk.gov.moj.cpp.progression.processor.document;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentAddedProcessor {

    public static final String PUBLIC_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";

    @Inject
    private Sender sender;

    @Handles("progression.event.court-document-added")
    public void handleCourtDocumentAddEvent(final JsonEnvelope envelope) {
        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject()).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(envelope));
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_ADDED).build();
        sender.send(JsonEnvelope.envelopeFrom(metadata, envelope.payload()));
    }

}
