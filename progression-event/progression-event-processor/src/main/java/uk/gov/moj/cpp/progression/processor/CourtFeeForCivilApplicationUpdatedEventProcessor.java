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
public class CourtFeeForCivilApplicationUpdatedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtFeeForCivilApplicationUpdatedEventProcessor.class);

    @Inject
    private Sender sender;

    @Handles("progression.event.court-fee-for-civil-application-updated")
    public void processCourtFeeForCivilApplication(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.court-fee-for-civil-application-updated", event.toObfuscatedDebugString());
        }
        sender.send(
                envelop(event.payloadAsJsonObject())
                        .withName("public.progression.court-fee-for-civil-application-updated")
                        .withMetadataFrom(event));

    }
}
