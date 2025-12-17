package uk.gov.moj.cpp.progression.event;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2629")
@ServiceComponent(EVENT_PROCESSOR)
public class DefenceCounselEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceCounselEventProcessor.class);
    private static final String PROGRESSION_COMMAND_HANDLER_ADD_HEARING_DEFENCE_COUNSEL="progression.command.handler.add-hearing-defence-counsel";
    private static final String PROGRESSION_COMMAND_HANDLER_UPDATE_HEARING_DEFENCE_COUNSEL="progression.command.handler.update-hearing-defence-counsel";
    private static final String PROGRESSION_COMMAND_HANDLER_REMOVE_HEARING_DEFENCE_COUNSEL="progression.command.handler.remove-hearing-defence-counsel";

    @Inject
    private Sender sender;

    @Handles("public.hearing.defence-counsel-added")
    public void hearingDefenceCounselAdded(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.defence-counsel-added {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_HANDLER_ADD_HEARING_DEFENCE_COUNSEL),envelope.payloadAsJsonObject()));
    }


    @Handles("public.hearing.defence-counsel-removed")
    public void hearingDefenceCounselRemoved(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.defence-counsel-removed {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_HANDLER_REMOVE_HEARING_DEFENCE_COUNSEL),envelope.payloadAsJsonObject()));
    }

    @Handles("public.hearing.defence-counsel-updated")
    public void hearingDefenceCounselUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.defence-counsel-updated {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_HANDLER_UPDATE_HEARING_DEFENCE_COUNSEL),envelope.payloadAsJsonObject()));
    }

}
