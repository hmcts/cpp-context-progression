package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingConfirmedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingConfirmedEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.hearing-initiate-enriched")
    public void processHearingInitiatedEnrichedEvent(final Envelope<Initiate> hearingInitiate) {
        final UUID hearingId = hearingInitiate.payload().getHearing().getId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.hearing-initiate-enriched in listener for hearing id {} ", hearingId);
        }
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);

        if (Objects.nonNull(hearingEntity)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding confirmed date for hearing with id: {} ", hearingId);
            }
                hearingEntity.setConfirmedDate(LocalDate.now());
                hearingRepository.save(hearingEntity);
        }
    }
}
