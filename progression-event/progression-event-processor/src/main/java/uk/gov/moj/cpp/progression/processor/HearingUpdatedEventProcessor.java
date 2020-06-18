package uk.gov.moj.cpp.progression.processor;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

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
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
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
        LOGGER.info("public.listing.hearing-updated event received with metadata {} and payload {}", jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingUpdated hearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdated.class);
        final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(jsonEnvelope, hearingUpdated.getUpdatedHearing().getId().toString());
        final JsonObject hearingJson = hearingPayloadOptional.orElseThrow(() -> new RuntimeException("Hearing not found")).getJsonObject("hearing");
        final Hearing hearingEntity = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final Hearing updatedHearing = progressionService.updateHearingForHearingUpdated(hearingUpdated.getUpdatedHearing(), jsonEnvelope, hearingEntity);

        final List<UUID> courtApplicationIds = hearingUpdated.getUpdatedHearing().getCourtApplicationIds();
        if (isNotEmpty(courtApplicationIds)) {
            progressionService.linkApplicationsToHearing(jsonEnvelope, updatedHearing, courtApplicationIds, HearingListingStatus.HEARING_INITIALISED);
        }
        if (isNotEmpty(hearingUpdated.getUpdatedHearing().getProsecutionCases())) {
            progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, updatedHearing);
        }
        progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, hearingUpdated);
    }
}
