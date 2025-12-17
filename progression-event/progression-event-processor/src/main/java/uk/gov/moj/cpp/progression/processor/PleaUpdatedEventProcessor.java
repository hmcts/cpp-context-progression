package uk.gov.moj.cpp.progression.processor;

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

@ServiceComponent(EVENT_PROCESSOR)
public class PleaUpdatedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaUpdatedEventProcessor.class);
    private static final String PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED = "public.hearing.hearing-offence-plea-updated";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_OFFENCE_PLEA = "progression.command.update-hearing-offence-plea";

    @Inject
    private Sender sender;

    @Handles(PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED)
    public void hearingPleaUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED + " {}", envelope.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_UPDATE_HEARING_OFFENCE_PLEA),
                envelope.payloadAsJsonObject()));
    }
}
