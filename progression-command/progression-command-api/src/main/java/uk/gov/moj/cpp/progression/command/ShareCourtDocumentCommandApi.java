package uk.gov.moj.cpp.progression.command;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(COMMAND_API)
public class ShareCourtDocumentCommandApi {

    @Inject
    private Sender sender;

    @Handles("progression.share-court-document")
    public void handle(final JsonEnvelope envelope) {
       final MetadataBuilder metadata = metadataFrom(envelope.metadata()).withName("progression.command.share-court-document");
        sender.send(envelopeFrom(metadata, envelope.payloadAsJsonObject()));
    }

    @Handles("progression.share-all-court-documents")
    public void handleShareAllCourtDocuments(final JsonEnvelope envelope) {
        final MetadataBuilder metadata = metadataFrom(envelope.metadata()).withName("progression.command.share-all-court-documents");
        sender.send(envelopeFrom(metadata, envelope.payloadAsJsonObject()));
    }
}
