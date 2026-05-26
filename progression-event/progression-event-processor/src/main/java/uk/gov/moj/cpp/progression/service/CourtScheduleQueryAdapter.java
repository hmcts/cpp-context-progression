package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls cpp-context-listing's {@code listing.query.court.schedule.draft.status} query to
 * decide whether any of a hearing's booked court-schedule sessions is DRAFT.
 *
 * <p>Used by {@code ListHearingRequestedProcessor} to detect unallocated CROWN hearings so
 * the courtroom info denormalised onto {@code courtCentre} by the UI can be stripped before
 * progression writes {@code hearing.payload} or sends the new-hearing notification email.
 * Mirrors the intent of listing-side
 * {@code HearingEnrichmentOrchestrator.stripRoomInfoIfAnyDraft} (SPRDT-858) so the strip
 * happens regardless of which side observes the payload first.
 *
 * <p>Goes through listing's query-api (not directly to listingcourtscheduler) so the cross-context
 * boundary stays clean - progression only ever talks to listing-context, never bypasses to
 * listingcourtscheduler-api. Fails-safe by returning {@code true} ("treat as DRAFT") if the
 * remote call returns an unexpected payload; leaking a phantom courtroom into a notification
 * email or persisted hearing snapshot is worse than dropping room info for what may be a
 * confirmed-allocated hearing.
 */
public class CourtScheduleQueryAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtScheduleQueryAdapter.class);

    private static final String LISTING_QUERY_COURT_SCHEDULE_DRAFT_STATUS = "listing.query.court.schedule.draft.status";
    private static final String COURT_SCHEDULE_ID_LIST = "courtScheduleIdList";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String ANY_DRAFT = "anyDraft";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public boolean anySessionIsDraft(final JsonEnvelope sourceEnvelope, final List<UUID> courtScheduleIds) {
        if (courtScheduleIds == null || courtScheduleIds.isEmpty()) {
            return false;
        }

        final JsonArrayBuilder list = createArrayBuilder();
        courtScheduleIds.forEach(id -> list.add(createObjectBuilder().add(COURT_SCHEDULE_ID, id.toString())));
        final JsonObject requestPayload = createObjectBuilder()
                .add(COURT_SCHEDULE_ID_LIST, list)
                .build();

        final JsonEnvelope response;
        try {
            response = requester.requestAsAdmin(enveloper
                    .withMetadataFrom(sourceEnvelope, LISTING_QUERY_COURT_SCHEDULE_DRAFT_STATUS)
                    .apply(requestPayload));
        } catch (Exception ex) {
            LOGGER.warn("listing.query.court.schedule.draft.status threw {} for {} ids - failing-safe by treating sessions as DRAFT",
                    ex.getClass().getSimpleName(), courtScheduleIds.size());
            return true;
        }

        final JsonObject responseBody = response == null ? null : response.payloadAsJsonObject();
        if (responseBody == null || !responseBody.containsKey(ANY_DRAFT) || responseBody.isNull(ANY_DRAFT)) {
            LOGGER.warn("listing.query.court.schedule.draft.status returned an unexpected payload for {} ids - failing-safe by treating sessions as DRAFT",
                    courtScheduleIds.size());
            return true;
        }

        return responseBody.getBoolean(ANY_DRAFT);
    }
}
