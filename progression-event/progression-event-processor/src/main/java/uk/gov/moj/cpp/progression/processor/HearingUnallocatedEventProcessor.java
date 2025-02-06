package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class HearingUnallocatedEventProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HearingUnallocatedEventProcessor.class.getName());
    private static final String PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED = "public.events.listing.hearing-unallocated";
    private static final String PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_UNALLOCATED_HEARING = "public.events.listing.offences-removed-from-unallocated-hearing";

    private static final String PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING = "public.events.listing.offences-removed-from-existing-unallocated-hearing";
    private static final String PROGRESSION_COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING = "progression.command.remove-offences-from-existing-hearing";


    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED)
    public void handleHearingUnallocatedPublicEvent(final JsonEnvelope jsonEnvelope) {
        log(PUBLIC_EVENTS_LISTING_HEARING_UNALLOCATED, jsonEnvelope);
        commandRemoveOffencesFromExistingHearing(jsonEnvelope, true);
    }

    @Handles(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_UNALLOCATED_HEARING)
    public void handleOffenceRemovedFromUnallocatedHearingPublicEvent(final JsonEnvelope jsonEnvelope) {
        log(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_UNALLOCATED_HEARING, jsonEnvelope);
        commandRemoveOffencesFromExistingHearing(jsonEnvelope, true);
    }

    @Handles(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING)
    public void handleOffencesRemovedFromExistingUnallocatedHearing(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} event received with metadata {} and payload {}",
                    PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING, jsonEnvelope.metadata(), jsonEnvelope.toObfuscatedDebugString());
        }

        commandRemoveOffencesFromExistingHearing(jsonEnvelope, false);
    }

    private void commandRemoveOffencesFromExistingHearing(final JsonEnvelope jsonEnvelope, final Boolean isNextHearingDeleting) {
        JsonObject payload ;
        if(Optional.ofNullable(isNextHearingDeleting).orElse(false)){
            payload = createObjectBuilder(jsonEnvelope.payloadAsJsonObject()).add("isNextHearingDeleting", true).build();
        }else{
            payload = jsonEnvelope.payloadAsJsonObject();
        }
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING),
                payload));
    }

    private void log(final String eventName, final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} event received with metadata {} and payload {}",
                    eventName, jsonEnvelope.metadata(), jsonEnvelope.toObfuscatedDebugString());
        }
    }
}
