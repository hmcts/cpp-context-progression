package uk.gov.moj.cpp.progression.casedocument.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class NewCaseDocumentReceivedListener {

    static final String
            PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.progression.case-document-added";
    private static final Logger LOG = LoggerFactory.getLogger(NewCaseDocumentReceivedListener.class);

    @Inject
    private  Sender sender;

    @Inject
    private Enveloper enveloper;




    @Handles("progression.events.new-case-document-received")
    public void processEvent(final JsonEnvelope envelope) {


        LOG.info("Sending new document received  command" + envelope.toString());

        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT).apply(envelope.payloadAsJsonObject()));

        LOG.info("Public event raised " + envelope);
    }


}
