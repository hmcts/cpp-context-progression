package uk.gov.moj.cpp.progression.processor;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import java.util.List;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedEventProcessor {

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingUpdatedEventProcessor.class.getName());

    @Handles("public.listing.hearing-updated")
    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("listing hearing updated Event Received metadata {} payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingUpdated hearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdated.class);
        final List<UUID> courtApplicationIds = hearingUpdated.getUpdatedHearing().getCourtApplicationIds();
        if(CollectionUtils.isNotEmpty(courtApplicationIds)){
            final Hearing hearing = progressionService.transformConfirmedHearing(hearingUpdated.getUpdatedHearing(), jsonEnvelope);
            progressionService.linkApplicationsToHearing(jsonEnvelope, hearing, courtApplicationIds, HearingListingStatus.HEARING_INITIALISED);
        }
        if(CollectionUtils.isNotEmpty(hearingUpdated.getUpdatedHearing().getProsecutionCases())) {
            progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, hearingUpdated);
        }
        progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, hearingUpdated);
    }
}
