package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import javax.inject.Inject;
import javax.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUpdatedEventProcessor.class.getName());

    private static final String PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED = "progression.command.process-hearing-updated";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Handles("public.listing.hearing-updated")
    public void processHearingUpdated(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("public.listing.hearing-updated event received with metadata {} and payload {}", jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingUpdated hearingUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdated.class);
        final ConfirmedHearing confirmedHearing = hearingUpdated.getUpdatedHearing();
        final List<UUID> courtApplicationIds = hearingUpdated.getUpdatedHearing().getCourtApplicationIds();
        if (isNotEmpty(courtApplicationIds)) {
            final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(jsonEnvelope, hearingUpdated.getUpdatedHearing().getId().toString());
            final JsonObject hearingJson = hearingPayloadOptional.orElseThrow(() -> new RuntimeException("Hearing not found")).getJsonObject("hearing");
            final Hearing hearingEntity = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            final Hearing updatedHearing = progressionService.updateHearingForHearingUpdated(hearingUpdated.getUpdatedHearing(), jsonEnvelope, hearingEntity);
            progressionService.linkApplicationsToHearing(jsonEnvelope, updatedHearing, courtApplicationIds, HearingListingStatus.HEARING_INITIALISED);
            if (isNotEmpty(hearingUpdated.getUpdatedHearing().getProsecutionCases())) {
                progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, updatedHearing);
            }
            progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, confirmedHearing);
        } else {
            sender.send(envelop(
                    createObjectBuilder()
                            .add("confirmedHearing", objectToJsonObjectConverter.convert(hearingUpdated.getUpdatedHearing()))
                            .build())
                    .withName(PROGRESSION_COMMAND_PROCESS_HEARING_UPDATED)
                    .withMetadataFrom(jsonEnvelope));
        }
    }

    @Handles("progression.event.hearing-updated-processed")
    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.hearing-updated-processed event received with  {}",  jsonEnvelope.toObfuscatedDebugString());
        }

        final HearingUpdatedProcessed hearingUpdatedProcessed = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUpdatedProcessed.class);
        final ConfirmedHearing confirmedHearing = hearingUpdatedProcessed.getConfirmedHearing();
        final Hearing updatedHearing = progressionService.updateHearingForHearingUpdated(confirmedHearing, jsonEnvelope, hearingUpdatedProcessed.getHearing());

        if (isNotEmpty(confirmedHearing.getProsecutionCases())) {
            progressionService.updateHearingListingStatusToHearingUpdate(jsonEnvelope, updatedHearing);
        }
        progressionService.publishHearingDetailChangedPublicEvent(jsonEnvelope, confirmedHearing);
    }

}
