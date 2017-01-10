package uk.gov.moj.cpp.progression.structure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class StructureDocumentAddedListener {

    private static final Logger LOG =
                    LoggerFactory.getLogger(StructureDocumentAddedListener.class.getName());


    @Handles("public.structure.case-document-added")
    public void processEvent(final JsonEnvelope envelope) {

        LOG.info("Received Structure Case Document added metadata "
                        + envelope.metadata().asJsonObject().toString());

    }
}
