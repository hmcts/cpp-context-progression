package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class PublicDefenceOrganisationAssociatedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicDefenceOrganisationAssociatedEventProcessor.class.getName());
    private static final String PROGRESSION_COMMAND_FOR_DEFENCE_ORGANISATION_ASSOCIATED = "progression.command.handler.associate-defence-organisation";
    private static final String IS_LAA = "isLAA";

    @Inject
    private Sender sender;

    @Handles("public.defence.defence-organisation-associated")
    public void processCommandForOrganisationAssociatedEvent(final JsonEnvelope event) {
        LOGGER.info("Consumed public.defence.defence-organisation-associated Event {}", event);
        final JsonObject requestJson = event.payloadAsJsonObject();

        final boolean isLAA = Boolean.parseBoolean(requestJson.containsKey(IS_LAA) ? requestJson.get(IS_LAA).toString() : "false");

        if(!isLAA){
            final Metadata metadata = metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_FOR_DEFENCE_ORGANISATION_ASSOCIATED).build();
            sender.send(envelopeFrom(metadata, removeProperty(requestJson, IS_LAA)));
        }
    }
}