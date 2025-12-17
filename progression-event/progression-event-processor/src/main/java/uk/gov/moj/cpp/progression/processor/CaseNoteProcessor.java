package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseNoteProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseNoteProcessor.class);
    private static final String LOG_OUTPUT_FORMAT = "Received '{}' event with payload {}";

    @Inject
    private Sender sender;

    @Handles("progression.event.case-note-added")
    public void processCaseNoteAdded(final JsonEnvelope event) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("recieved private  event progression.event.case-note-added correlationId: {}", event.metadata().clientCorrelationId().orElse(null));
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, "progression.event.case-note-added", event.toObfuscatedDebugString());
        }
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("raising public  event public.progression.case-note-added correlationId: {}", event.metadata().clientCorrelationId().orElse(null));
        }
        sender.send(
                envelop(Json.createObjectBuilder().build())
                        .withName("public.progression.case-note-added")
                        .withMetadataFrom(event));
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("raised public  event public.progression.case-note-added correlationId: {}", event.metadata().clientCorrelationId().orElse(null));
        }
    }

    @Handles("progression.event.case-note-added-v2")
    public void processCaseNoteAddedV2(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, "progression.event.case-note-added-v2", event.toObfuscatedDebugString());
        }
        sender.send(
                envelop(Json.createObjectBuilder().build())
                        .withName("public.progression.case-note-added")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.case-note-edited")
    public void processCaseNoteEdited(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, "progression.event.case-note-edited", event.toObfuscatedDebugString());
        }
        sender.send(
                envelop(Json.createObjectBuilder().build())
                        .withName("public.progression.case-note-edited")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.case-note-edited-v2")
    public void processCaseNoteEditedV2(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, "progression.event.case-note-edited-v2", event.toObfuscatedDebugString());
        }
        sender.send(
                envelop(Json.createObjectBuilder().build())
                        .withName("public.progression.case-note-edited")
                        .withMetadataFrom(event));
    }
}
