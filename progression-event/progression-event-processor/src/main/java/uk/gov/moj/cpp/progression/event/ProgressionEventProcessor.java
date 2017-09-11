package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

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

@SuppressWarnings("WeakerAccess")
@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionEventProcessor.class.getCanonicalName());
    private static final String PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED = "public.progression.events.sentence-hearing-date-added";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_UPDATED = "public.progression.events.sentence-hearing-date-updated";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_ADDED = "public.progression.events.sentence-hearing-added";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT = "public.progression.events.case-added-to-crown-court";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS = "public.progression.events.case-already-exists-in-crown-court";
    public static final String CASE_ID = "caseId";
    public static final String CASE_PROGRESSION_ID = "caseProgressionId";
    public static final String COURT_CENTRE_ID ="courtCentreId";
    public static final String STATUS ="status";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.events.sentence-hearing-date-added")
    public void publishSentenceHearingAddedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED).apply(payload));
    }

    @Handles("progression.events.sentence-hearing-date-updated")
    public void publishSentenceHearingUpdatedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_UPDATED).apply(payload));
    }

    @Handles("progression.events.sentence-hearing-added")
    public void publishSentenceHearingIdAddedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_ADDED).apply(payload));
    }

    @Handles("progression.events.case-added-to-crown-court")
    public void publishCaseAddedToCrownCourtPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        LOGGER.debug("Raising public event for case added to crown court for caseId: " +caseId);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).
        add(CASE_PROGRESSION_ID,event.payloadAsJsonObject().getString(CASE_PROGRESSION_ID)).
        add(STATUS,event.payloadAsJsonObject().getString(STATUS)).
        add(COURT_CENTRE_ID,event.payloadAsJsonObject().getString(COURT_CENTRE_ID)).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT).apply(payload));
    }

    @Handles("progression.events.case-already-exists-in-crown-court")
    public void publishCaseAlreadyExistsInCrownCourtEvent(JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS).apply(payload));
    }
}
