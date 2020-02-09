package uk.gov.moj.cpp.progression.processor;

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
public class DefendantDefenceOrganisationDisassociatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDefenceOrganisationDisassociatedProcessor.class.getName());

    private static final String PROGRESSION_COMMAND_DISASSOCIATE_DEFENCE_ORGANISATION = "progression.command.handler.disassociate-defence-organisation";


    @Inject
    private Sender sender;


    @Handles("progression.event.defendant-defence-organisation-disassociated")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("Received Defendant DefenceOrganisationDisassociated Event {}", event);
        sender.send(
                Enveloper.envelop(event.payloadAsJsonObject())
                        .withName(PROGRESSION_COMMAND_DISASSOCIATE_DEFENCE_ORGANISATION)
                        .withMetadataFrom(event));
    }
}
