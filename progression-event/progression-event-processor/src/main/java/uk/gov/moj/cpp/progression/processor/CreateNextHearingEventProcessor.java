package uk.gov.moj.cpp.progression.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@ServiceComponent(EVENT_PROCESSOR)
public class CreateNextHearingEventProcessor {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNextHearingEventProcessor.class.getName());


    @Handles("public.listing.create-next-hearing-requested")
    public void processCreateNextHearing(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.listing.create-next-hearing-requested' event with payload: {}", event.toObfuscatedDebugString());
        }
        sender.send(envelop(event.payloadAsJsonObject().getJsonObject("createNextHearing")).withName("progression.command.create-next-hearing").withMetadataFrom(event));
    }
}
