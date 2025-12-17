package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    public void sendMessage(final JsonEnvelope jsonEnvelope, final JsonObject jsonObject, final String messageName) {
        LOGGER.info("Raising a public message {}",messageName);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, messageName).apply(jsonObject));
    }
}