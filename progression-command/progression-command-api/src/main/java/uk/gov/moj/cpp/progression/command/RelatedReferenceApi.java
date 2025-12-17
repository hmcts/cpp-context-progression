package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class RelatedReferenceApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedReferenceApi.class);

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Sender sender;

    @Handles("progression.add-related-reference")
    public void handleAddRelatedReference(final JsonEnvelope envelope) {
        LOGGER.info("Received request in progression.add-related-reference");
        sender.send(Enveloper.envelop(envelope.payload())
                .withName("progression.command.add-related-reference")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.delete-related-reference")
    public void handleDeleteRelatedReference(final JsonEnvelope envelope) {
        LOGGER.info("Received request in progression.delete-related-reference");
        sender.send(Enveloper.envelop(envelope.payload())
                .withName("progression.command.delete-related-reference")
                .withMetadataFrom(envelope));
    }

}
