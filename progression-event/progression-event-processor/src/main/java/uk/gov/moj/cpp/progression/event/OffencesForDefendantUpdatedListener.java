package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_PROCESSOR)
public class OffencesForDefendantUpdatedListener {

    static final String PUBLIC_STRUCTURE_EVENTS_OFFENCES_FOR_DEFENDANT_UPDATED = "public.progression.events.offences-for-defendant-updated";
    private static final Logger LOGGER = LoggerFactory.getLogger(OffencesForDefendantUpdatedListener.class.getCanonicalName());

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.events.offences-for-defendant-updated")
    public void handleOffencesForDefendantUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        final JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.debug("Received offences for defendant updated for caseId: " + privateEventPayload.getString(CASE_ID));

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, privateEventPayload.getString(CASE_ID))
                .add(DEFENDANT_ID, privateEventPayload.getString(DEFENDANT_ID)).build();

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_STRUCTURE_EVENTS_OFFENCES_FOR_DEFENDANT_UPDATED).apply(publicEventPayload));
    }



}