package uk.gov.justice.api.resource;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;

@ServiceComponent(Component.COMMAND_API)
public class UploadFileServiceSender {


    @Inject
    private Sender sender;

    private static final String SINGLE_DOCUMENT_COMMAND = "progression.command.upload-case-documents";

    public void doSend(final JsonObject objectToSend, final String userId, final String session,
                       final String correlationId) {

        Metadata metadataAsJsonObject = JsonObjectMetadata.metadataOf(UUID.randomUUID(), SINGLE_DOCUMENT_COMMAND).withUserId(userId)
                .withSessionId(session).withClientCorrelationId(correlationId).build();

        JsonEnvelope envelope = DefaultJsonEnvelope.envelopeFrom(
                JsonObjectMetadata.metadataFrom(metadataAsJsonObject), objectToSend);

        uploadCaseDocumentHandler(envelope);

    }

    @Handles(SINGLE_DOCUMENT_COMMAND)
    public void uploadCaseDocumentHandler(JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
