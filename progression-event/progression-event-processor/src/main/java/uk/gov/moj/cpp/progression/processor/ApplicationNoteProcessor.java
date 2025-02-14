package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationNoteProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationNoteProcessor.class);

    @Inject
    private Sender sender;

    @Handles("progression.event.application-note-added")
    public void processApplicationNoteAdded(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received event progression.event.application-note-added with payload {}",
                    event.toObfuscatedDebugString());
        }

        sender.send(envelop(event.payloadAsJsonObject())
                .withName("public.progression.application-note-added")
                .withMetadataFrom(event));
    }

    @Handles("progression.event.application-note-edited")
    public void processApplicationNoteEdited(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received event progression.event.application-note-edited with payload {}",
                    event.toObfuscatedDebugString());
        }

        sender.send(envelop(event.payloadAsJsonObject())
                .withName("public.progression.application-note-edited")
                .withMetadataFrom(event));
    }
}
