package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

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
public class HearingNowsEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingNowsEventProcessor.class.getName());
    private static final String PROGRESSION_COMMAND_FOR_NOW_NOTIFICATION_GENERATED = "progression.command.record-now-notification-generated";

    @Inject
    Sender sender;

    @Handles("public.hearingnows.now-notification-generated")
    public void processNowNotificationGeneratedEvent(final JsonEnvelope event) {
        LOGGER.info("Consumed public.hearingnows.now-notification-generated event {}", event.toObfuscatedDebugString());
        final JsonObject requestJson = event.payloadAsJsonObject();

        final Metadata metadata = metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_FOR_NOW_NOTIFICATION_GENERATED).build();
        sender.send(envelopeFrom(metadata, requestJson));
    }
}
