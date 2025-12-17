package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

@ServiceComponent(EVENT_LISTENER)
public class HmiEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(HmiEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;


    @Handles("progression.event.hearing-moved-to-unallocated")
    public void handleHearingMovedToUnallocated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.hearing-moved-to-unallocated {} ", event.toObfuscatedDebugString());
        }

        final HearingMovedToUnallocated hearingMovedToUnallocated = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingMovedToUnallocated.class);

        final HearingEntity currentHearingEntity = hearingRepository.findBy(hearingMovedToUnallocated.getHearing().getId());
        final String hearingPayload = objectToJsonObjectConverter.convert(hearingMovedToUnallocated.getHearing()).toString();

        currentHearingEntity.setPayload(hearingPayload);
        currentHearingEntity.setListingStatus(HearingListingStatus.SENT_FOR_LISTING);
        currentHearingEntity.setConfirmedDate(null);
        hearingRepository.save(currentHearingEntity);

    }
}
