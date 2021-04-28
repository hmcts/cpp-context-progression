package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedEventProcessor {

    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED = "public.progression.hearing-resulted";

    @Inject
    private Sender sender;

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventProcessor.class.getName());

    @Handles("progression.event.hearing-resulted")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("progression.event.hearing-resulted event received with metadata {} and payload {}", event.metadata(), event.payloadAsJsonObject());
        final Metadata metadata = metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_HEARING_RESULTED).build();

        final JsonObject outboundPayload = createObjectBuilder()
                .add("hearing", event.payloadAsJsonObject().getJsonObject("hearing"))
                .add("sharedTime", event.payloadAsJsonObject().getJsonString("sharedTime"))
                .build();

        sender.send(envelopeFrom(metadata, outboundPayload));
    }
}

