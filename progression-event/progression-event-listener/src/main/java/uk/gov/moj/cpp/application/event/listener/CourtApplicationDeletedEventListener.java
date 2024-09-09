package uk.gov.moj.cpp.application.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import uk.gov.justice.core.courts.CourtApplicationHearingDeleted;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CourtApplicationDeletedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationDeletedEventListener.class);

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.court-application-hearing-deleted")
    public void processCourtApplicationDeletedEvent(final Envelope<CourtApplicationHearingDeleted> event) {
        final UUID applicationId = event.payload().getApplicationId();
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received event '{}' application: {}", "progression.event.court-application-deleted", applicationId);
        }
        hearingApplicationRepository.removeByApplicationId(applicationId);
        courtApplicationCaseRepository.removeByApplicationId(applicationId);
        courtApplicationRepository.removeByApplicationId(applicationId);
        hearingRepository.removeByHearingId(hearingId);
    }
}
