package uk.gov.moj.cpp.progression.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantAllocationDecisionUpdatedListener {

    static final String PUBLIC_STRUCTURE_EVENTS_DEFENDANT_ALLOCATION_DECISION_UPDATED = "public.progression.events.defendant-allocation-decision-updated";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantAllocationDecisionUpdatedListener.class.getCanonicalName());

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("progression.events.defendant-allocation-decision-updated")
    public void handleAllocationDecisionUpdatedEvent(JsonEnvelope jsonEnvelope) {
        JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.debug("Received defendant allocation decision updated for caseId: " + privateEventPayload.getString(CASE_ID));

        JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, privateEventPayload.getString(CASE_ID))
                .add(DEFENDANT_ID, privateEventPayload.getString(DEFENDANT_ID)).build();

        sender.send(enveloper
                .withMetadataFrom(jsonEnvelope, PUBLIC_STRUCTURE_EVENTS_DEFENDANT_ALLOCATION_DECISION_UPDATED)
                .apply(publicEventPayload));
    }



}