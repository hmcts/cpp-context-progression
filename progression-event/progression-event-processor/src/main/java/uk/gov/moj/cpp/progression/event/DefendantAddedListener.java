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
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DESCRIPTION;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantAddedListener {

    static final String DEFENDANT_ADDED_PUBLIC_EVENT = "public.progression.defendant-added";
    static final String DEFENDANT_ADDITION_FAILED_PUBLIC_EVENT = "public.progression.defendant-addition-failed";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantAddedListener.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.events.defendant-added")
    public void handleDefendantAddedEvent(final JsonEnvelope jsonEnvelope) {
        JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        final String caseId = privateEventPayload.getString(CASE_ID);
        final String defendantId = privateEventPayload.getString(DEFENDANT_ID);

        LOGGER.debug("Defendant with ID '{}' added for case with ID '{}' ", defendantId, caseId);

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, DEFENDANT_ADDED_PUBLIC_EVENT).apply(jsonEnvelope));
    }

    @Handles("progression.events.defendant-addition-failed")
    public void handleDefendantAdditionFailedEvent(final JsonEnvelope jsonEnvelope) {
        JsonObject privateEventPayload = jsonEnvelope.payloadAsJsonObject();
        String caseId = privateEventPayload.getString(CASE_ID);
        String defendantId = privateEventPayload.getString(DEFENDANT_ID);
        String description = privateEventPayload.getString(DESCRIPTION);

        LOGGER.debug("Defendant addition failed for defendant ID: {}", defendantId);

        JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId)
                .add(DESCRIPTION, description).build();

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, DEFENDANT_ADDITION_FAILED_PUBLIC_EVENT).apply(publicEventPayload));
    }
}
