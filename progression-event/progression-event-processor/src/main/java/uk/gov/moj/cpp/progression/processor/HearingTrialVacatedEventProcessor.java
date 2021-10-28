package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import javax.json.JsonObjectBuilder;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingTrialVacatedEventProcessor {

    private static final String PROGRESSION_COMMAND_FOR_TRIAL_VACATED = "progression.command.hearing-trial-vacated";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingTrialVacatedEventProcessor.class.getName());
    private static final String VACATED_TRIAL_REASON_ID = "vacatedTrialReasonId";
    @Inject
    ProgressionService progressionService;
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("public.hearing.trial-vacated")
    public void handleHearingTrialVacatedEvent(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.hearing.trial-vacated event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_FOR_TRIAL_VACATED),
                payload));
    }

    @Handles("public.listing.vacated-trial-updated")
    public void handleListingTrialVacatedEvent(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.listing.vacated-trial-updated event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final JsonObjectBuilder trialVacatedCommandBuilder = Json.createObjectBuilder()
                        .add("hearingId", payload.getString("hearingId"));
        if(payload.containsKey(VACATED_TRIAL_REASON_ID)) {
            trialVacatedCommandBuilder.add(VACATED_TRIAL_REASON_ID, payload.getString(VACATED_TRIAL_REASON_ID));
        }
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_FOR_TRIAL_VACATED),
                trialVacatedCommandBuilder.build()));
    }

    @Handles("progression.event.hearing-trial-vacated")
    public void hearingTrialVacatedEvent(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-trial-vacated {} ", jsonEnvelope.toObfuscatedDebugString());
        }
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final HearingTrialVacated hearingTrialVacated = jsonObjectToObjectConverter.convert(payload, HearingTrialVacated.class);


        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, hearingTrialVacated.getHearingId());
    }

}
