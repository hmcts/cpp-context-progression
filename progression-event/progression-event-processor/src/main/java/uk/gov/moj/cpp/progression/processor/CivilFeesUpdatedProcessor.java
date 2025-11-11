package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.CivilFeeResults;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CivilFeesUpdatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CivilFeesUpdatedProcessor.class);

    @Inject
    private Sender sender;

    @Handles("progression.event.civil-fees-updated")
    public void processCivilFeesUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Civil Fees payload - {}", envelope.payloadAsJsonObject());
        }

        final Envelope<JsonObject> responseEventPayload = Enveloper.envelop(createResponsePayload(CivilFeeResults.SUCCESS))
                .withName("public.progression.civil-fees-response")
                .withMetadataFrom(envelope);
        sender.send(responseEventPayload);
        LOGGER.info("Civil Fees response event payload - {}", responseEventPayload.payload());
    }

    @Handles("progression.event.civil-fees-added")
    public void processCivilFeesAdded(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Civil Fees payload - {}", envelope.payloadAsJsonObject());
        }

        final Envelope<JsonObject> responseEventPayload = Enveloper.envelop(createResponsePayload(CivilFeeResults.SUCCESS))
                .withName("public.progression.civil-fees-response")
                .withMetadataFrom(envelope);
        sender.send(responseEventPayload);
        LOGGER.info("Civil Fees response event payload - {}", responseEventPayload.payload());
    }

    private static JsonObject createResponsePayload(final CivilFeeResults response) {
        return Json.createObjectBuilder()
                .add("civilFeeResults", response.toString())
                .build();
    }
}
