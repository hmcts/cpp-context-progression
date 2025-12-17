package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;


import java.util.UUID;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingDaysWithoutCourtCentreCorrectedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDaysWithoutCourtCentreCorrectedProcessor.class.getName());

    @Inject
    private ProgressionService progressionService;


    @Handles("progression.events.hearing-days-without-court-centre-corrected")
    public void correctHearingDaysWithoutCourtCentre(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.events.hearing-days-without-court-centre-corrected event received with  {}",  jsonEnvelope.toObfuscatedDebugString());
        }
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString("id")));
    }

}
