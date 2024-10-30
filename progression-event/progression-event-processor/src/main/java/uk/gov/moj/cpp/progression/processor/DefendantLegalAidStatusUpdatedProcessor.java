package uk.gov.moj.cpp.progression.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantLegalAidStatusUpdatedProcessor {

    protected static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantLegalAidStatusUpdatedProcessor.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;


    @Handles("progression.event.defendant-legalaid-status-updated-v2")
    public void handleDefendantLegalAidStatusUpdatedV2(final JsonEnvelope jsonEnvelope) {
        handle(jsonEnvelope);
    }

    private void handle(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received Defendant Legal Status updated with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).apply(jsonEnvelope.payloadAsJsonObject()));
    }

    @Handles("progression.event.defendant-legalaid-status-updated")
    public void handleDefendantLegalAidStatusUpdated(final JsonEnvelope jsonEnvelope) {
        handle(jsonEnvelope);
    }
}
