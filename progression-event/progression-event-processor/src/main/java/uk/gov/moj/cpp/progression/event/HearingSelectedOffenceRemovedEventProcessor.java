package uk.gov.moj.cpp.progression.event;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingSelectedOffenceRemovedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingSelectedOffenceRemovedEventProcessor.class.getName());
    private static final String PROGRESSION_COMMAND_FOR_REMOVE_OFFENCE_FROM_HEARING_EXISTING_HEARING = "progression.command.remove-offences-from-existing-hearing";
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("public.events.listing.offences-removed-from-allocated-hearing")
    public void handleHearingSelectedOffenceRemovedFromExistingHearingPublicEvent(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("public.events.listing.offences-removed-from-allocated-hearing event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_FOR_REMOVE_OFFENCE_FROM_HEARING_EXISTING_HEARING),
                payload));
    }
}
