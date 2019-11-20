package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefenceOrganisationAssociatedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceOrganisationAssociatedEventProcessor.class.getName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.defence-organisation-associated")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("Received DefenceOrganisationAssociated Event {}", event);
        sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("public.progression.defence-organisation-associated")
                .withMetadataFrom(event));
    }

}
