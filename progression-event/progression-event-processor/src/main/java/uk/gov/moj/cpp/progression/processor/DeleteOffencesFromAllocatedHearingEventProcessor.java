package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class DeleteOffencesFromAllocatedHearingEventProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DeleteOffencesFromAllocatedHearingEventProcessor.class.getName());

    private static final String PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-existing-allocated-hearing";
    private static final String PROGRESSION_COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING = "progression.command.remove-offences-from-existing-hearing";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING)
    public void handleOffencesRemovedFromExistingAllocatedHearingPublicEvent(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} event received with metadata {} and payload {}",
                    PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING, jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        }

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING),
                jsonEnvelope.payloadAsJsonObject()));
    }
}
