package uk.gov.moj.cpp.progression.casedocument.listener;

import static java.lang.String.format;
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
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class NewCaseDocumentReceivedListener {

    private static final Logger LOG = LoggerFactory.getLogger(NewCaseDocumentReceivedListener.class);

    private static final String STRUCTURE_COMMAND_ADD_DOCUMENT = "structure.command.add-case-document";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;
    
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
  
    private static final String PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.progression.case-document-added";


    @Handles("progression.events.new-case-document-received")
    public void processEvent(final JsonEnvelope envelope) {

        LOG.info(format(
                        "NewCaseDocumentReceivedListener:Received Document upload request from userId= %s sessionId= %s correlationId= %s",
                        envelope.metadata().userId().get(),
                        envelope.metadata().sessionId().get(),
                        envelope.metadata().clientCorrelationId().get()));

        LOG.info("Sending new document received structure command" + envelope.toString());

        sendStructureCommand(envelope);

        LOG.info("Structure command to upload a document complete " + envelope);

        LOG.info("Sending uploaded ");
        sendPublicEvent(envelope);
        
        LOG.info("Public event raised " + envelope);
    }


    private void sendStructureCommand(final JsonEnvelope envelope) {

        final NewCaseDocumentReceivedEvent event = jsonObjectConverter.convert(
                envelope.payloadAsJsonObject(), NewCaseDocumentReceivedEvent.class);

        final AssociateNewCaseDocumentCommand associateNewCaseDocumentCommand =
                        new AssociateNewCaseDocumentCommand(event.getCppCaseId().toString(),
                                        event.getFileId(), "PLEA");


        final Metadata metadata = JsonObjectMetadata
                        .metadataOf(UUID.randomUUID(), STRUCTURE_COMMAND_ADD_DOCUMENT)
                        .withSessionId(envelope.metadata().sessionId().get())
                        .withClientCorrelationId(envelope.metadata().clientCorrelationId().get())
                        .withUserId(envelope.metadata().userId().get()).build();

        sender.send(envelopeFrom(metadata, objectToJsonObjectConverter.convert(associateNewCaseDocumentCommand)));

    }


    private void sendPublicEvent(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT).apply(envelope.payloadAsJsonObject()));
    }
}
