package uk.gov.moj.cpp.progression.event;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantUpdatedListener {

    static final String DEFENDANT_UPDATED_PUBLIC_EVENT = "public.progression.events.defendant-updated";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantUpdatedListener.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.events.defendant-updated")
    public void handleDefendantUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        final String caseId = privateEventPayload.getString(CASE_ID);
        final String defendantId = privateEventPayload.getString(DEFENDANT_ID);

        LOGGER.debug("Defendant with ID '{}' updated for case with ID '{}' ", defendantId, caseId);

        JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId).build();

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, DEFENDANT_UPDATED_PUBLIC_EVENT).apply(publicEventPayload));
    }


}
