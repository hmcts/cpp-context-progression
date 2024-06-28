package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.court.HearingResultedBdf;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingResultedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.hearing-resulted-bdf")
    public void processHearingInitiatedEnrichedEvent(final Envelope<HearingResultedBdf> resultHearingBdfEnvelope) {
        final UUID hearingId = resultHearingBdfEnvelope.payload().getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.hearing-resulted-by-bdf in listener for hearing id {} ", hearingId);
        }
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);

        if (Objects.nonNull(hearingEntity)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("resulting hearing with id: {} ", hearingId);
            }
                hearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
                hearingRepository.save(hearingEntity);
        }
    }
}
