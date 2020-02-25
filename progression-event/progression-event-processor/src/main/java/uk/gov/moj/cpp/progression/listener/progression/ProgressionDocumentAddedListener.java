package uk.gov.moj.cpp.progression.listener.progression;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.listener.casedocument.AddMaterialCommand;
import uk.gov.moj.cpp.progression.listener.casedocument.Document;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(Component.EVENT_PROCESSOR)
public class ProgressionDocumentAddedListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressionDocumentAddedListener.class.getName());


    private static final String MATERIAL_COMMAND_ADD_MATERIAL = "material.add-material";

    private final Sender sender;

    private final Enveloper enveloper;

    @Inject
    public ProgressionDocumentAddedListener(final Sender sender, final Enveloper enveloper) {
        this.sender = sender;
        this.enveloper = enveloper;
    }


    @Handles("public.progression.case-document-added")
    public void processEvent(final JsonEnvelope envelope) {

        LOG.info("ProgressionDocumentAddedListener:Received Progression Case Document added metadata {}", envelope);

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String fileId = payload.getString("fileId");
        final String fileName = payload.getString("fileName");
        final String mimeType = payload.getString("fileMimeType");

        final AddMaterialCommand command = new AddMaterialCommand(fileId, new Document(fileId, mimeType), fileName);

        sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, MATERIAL_COMMAND_ADD_MATERIAL).apply(command));

        LOG.info("Material command to upload a document complete {}", envelope);

    }
}
