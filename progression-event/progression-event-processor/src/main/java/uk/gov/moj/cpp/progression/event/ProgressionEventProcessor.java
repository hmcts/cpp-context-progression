package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionEventProcessor {

    private static final String PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED = "public.progression.events.sentence-hearing-date-added";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.events.sentence-hearing-date-added")
    public void publishCaseStartedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString("caseId");
        final JsonObject payload = Json.createObjectBuilder().add("caseId", caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED).apply(payload));
    }


}
