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
public class HearingResultedCaseUpdatedProcessor {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedCaseUpdatedProcessor.class.getCanonicalName());


    static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED= "public.progression.hearing-resulted-case-updated";

    @Handles("progression.event.hearing-resulted-case-updated")
    public void process(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received Hearing Resulted Case Updated with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED)
                .apply(jsonEnvelope.payload()));
    }


}
