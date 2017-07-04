package uk.gov.moj.cpp.progression.casedocument.listener;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class NewCaseDocumentReceivedListener {

    static final String PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.progression.case-document-added";
    static final String STRUCTURE_COMMAND_ADD_DOCUMENT = "structure.command.add-case-document";
    private static final Logger LOG = LoggerFactory.getLogger(NewCaseDocumentReceivedListener.class);
    private final Sender sender;

    private final Enveloper enveloper;

    @Inject
    public NewCaseDocumentReceivedListener(Sender sender, Enveloper enveloper) {
        this.sender = sender;
        this.enveloper = enveloper;
    }


    @Handles("progression.events.new-case-document-received")
    public void processEvent(final JsonEnvelope envelope) {


        LOG.info("Sending new document received structure command" + envelope.toString());

        JsonObject payload = envelope.payloadAsJsonObject();

        UUID cppCaseId = UUID.fromString(payload.getString("cppCaseId"));
        String fileId = payload.getString("fileId");


        final AssociateNewCaseDocumentCommand associateNewCaseDocumentCommand = new AssociateNewCaseDocumentCommand(cppCaseId.toString(), fileId, "PLEA");


        sender.send(enveloper.withMetadataFrom(envelope, STRUCTURE_COMMAND_ADD_DOCUMENT).apply(associateNewCaseDocumentCommand));

        LOG.info("Structure command to upload a document complete " + envelope);

        LOG.info("Sending uploaded ");
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT).apply(envelope.payloadAsJsonObject()));

        LOG.info("Public event raised " + envelope);
    }


}
