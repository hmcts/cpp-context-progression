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
import javax.json.JsonObject;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefendantDefenceAssociationLockedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDefenceAssociationLockedProcessor.class.getName());

    private static final String PUBLIC_PROGRESSION_LOCK_DEFENCE_ASSOCIATION_FOR_LAA = "public.progression.defence-association-for-laa-locked";

    @Inject
    private Sender sender;


    @Handles("progression.event.defendant-defence-association-locked")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("Received progression.event.defendant-defence-association-locked Event {}", event.payloadAsJsonObject());
        final JsonObject payload = event.payloadAsJsonObject();
        sender.send(
                Enveloper.envelop(payload)
                        .withName(PUBLIC_PROGRESSION_LOCK_DEFENCE_ASSOCIATION_FOR_LAA)
                        .withMetadataFrom(event));

    }
}
