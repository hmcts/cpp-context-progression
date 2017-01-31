package uk.gov.moj.cpp.progression.structure.listener;

import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.casedocument.listener.AddMaterialCommand;
import uk.gov.moj.cpp.progression.casedocument.listener.Document;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class ProgressionDocumentAddedListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressionDocumentAddedListener.class.getName());


    private static final String MATERIAL_COMMAND_ADD_MATERIAL = "material.add-material";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("public.progression.case-document-added")
    public void processEvent(final JsonEnvelope envelope) {

        try {
            LOG.info("ProgressionDocumentAddedListener:Received Progression Case Document added metadata "
                            + envelope.metadata().asJsonObject().toString());

            sendAddMaterialCommand(envelope);

            LOG.info("Material command to upload a document complete " + envelope);


        } catch (Exception e) {

            LOG.info("Material command to upload a document failed ");

            LOG.error(envelope.toString(), e);
        }
    }

    private void sendAddMaterialCommand(final JsonEnvelope envelope) {

        final NewCaseDocumentReceivedEvent event = jsonObjectConverter.convert(
                        envelope.payloadAsJsonObject(), NewCaseDocumentReceivedEvent.class);

        final Document document = new Document(event.getFileId(), event.getFileMimeType());

        final AddMaterialCommand command =
                        new AddMaterialCommand(event.getFileId(), document, event.getFileName());

        final Metadata metadata = JsonObjectMetadata
                        .metadataOf(UUID.randomUUID(), MATERIAL_COMMAND_ADD_MATERIAL)
                        .withSessionId(envelope.metadata().sessionId().get())
                        .withClientCorrelationId(envelope.metadata().clientCorrelationId().get())
                        .withUserId(envelope.metadata().userId().get()).build();

        sender.send(envelopeFrom(metadata, objectToJsonObjectConverter.convert(command)));

    }

}
