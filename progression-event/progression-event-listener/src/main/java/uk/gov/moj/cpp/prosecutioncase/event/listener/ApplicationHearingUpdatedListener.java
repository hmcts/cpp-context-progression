package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.event.ApplicationHearingDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class ApplicationHearingUpdatedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedEventListener.class);
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private HearingRepository hearingRepository;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.application-hearing-defendant-updated")
    public void processUpdateDefendantOnApplicationHearing(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event in listener progression.event.application-hearing-defendant-updated {} ", event.toObfuscatedDebugString());
        }
        final ApplicationHearingDefendantUpdated hearingDefendantUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationHearingDefendantUpdated.class);
        final Hearing updatedHearing = hearingDefendantUpdated.getHearing();
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingDefendantUpdated.getHearing().getId());
        if(nonNull(hearingEntity)) {
            final HearingEntity updatedHearingEntity = new HearingEntity();
            updatedHearingEntity.setHearingId(hearingEntity.getHearingId());
            updatedHearingEntity.setListingStatus(hearingEntity.getListingStatus());
            updatedHearingEntity.setConfirmedDate(hearingEntity.getConfirmedDate());
            updatedHearingEntity.setResultLines(hearingEntity.getResultLines());
            updatedHearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
            hearingRepository.save(updatedHearingEntity);
        }
    }
}
