package uk.gov.justice.api.resource;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ServiceComponent(Component.COMMAND_API)
public class UploadFileServiceSender {


    @Inject
    private Sender sender;

    private static final String SINGLE_DOCUMENT_COMMAND = "progression.command.upload-case-documents";

    public void doSend(final JsonObject objectToSend, final String userId, final String correlationId) {

        final Metadata metadataAsJsonObject = JsonObjectMetadata.metadataOf(UUID.randomUUID(), SINGLE_DOCUMENT_COMMAND).withUserId(userId)
                .withClientCorrelationId(correlationId).build();

        final JsonEnvelope envelope = DefaultJsonEnvelope.envelopeFrom(
                JsonObjectMetadata.metadataFrom(metadataAsJsonObject), objectToSend);

        uploadCaseDocumentHandler(envelope);

    }

    @Handles(SINGLE_DOCUMENT_COMMAND)
    public void uploadCaseDocumentHandler(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
