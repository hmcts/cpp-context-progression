package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingDeletedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDeletedEventProcessor.class.getName());
    private static final String PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED = "public.events.listing.allocated-hearing-deleted";
    private static final String PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED = "public.events.listing.unallocated-hearing-deleted";
    private static final String PROGRESSION_EVENT_HEARING_DELETED = "progression.event.hearing-deleted";
    private static final String PROGRESSION_COMMAND_DELETE_HEARING = "progression.command.delete-hearing";
    private static final String PROGRESSION_COMMAND_DELETE_HEARING_FOR_PROSECUTION_CASE = "progression.command.delete-hearing-for-prosecution-case";
    private static final String PROGRESSION_COMMAND_DELETE_HEARING_FOR_COURT_APPLICATION = "progression.command.delete-hearing-for-court-application";

    private static final String HEARING_ID = "hearingId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String COURT_APPLICATION_ID = "courtApplicationId";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED)
    public void handleAllocatedHearingDeletedPublicEvent(final JsonEnvelope jsonEnvelope) {

        logEvent(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED, jsonEnvelope);

        sendCommandDeleteHearing(jsonEnvelope);
    }

    @Handles(PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED)
    public void handleUnallocatedHearingDeletedPublicEvent(final JsonEnvelope jsonEnvelope) {

        logEvent(PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED, jsonEnvelope);

        sendCommandDeleteHearing(jsonEnvelope);
    }

    @Handles(PROGRESSION_EVENT_HEARING_DELETED)
    public void handleHearingDeletedPrivateEvent(final JsonEnvelope jsonEnvelope) {

        logEvent(PROGRESSION_EVENT_HEARING_DELETED, jsonEnvelope);

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final HearingDeleted hearingDeleted = jsonObjectToObjectConverter.convert(payload, HearingDeleted.class);

        if (isNotEmpty(hearingDeleted.getProsecutionCaseIds())) {
            hearingDeleted.getProsecutionCaseIds().forEach(prosecutionCaseId ->
                    sendCommandDeleteHearingForProsecutionCase(jsonEnvelope, hearingDeleted.getHearingId(), prosecutionCaseId)
            );
        }

        if (isNotEmpty(hearingDeleted.getCourtApplicationIds())) {
            hearingDeleted.getCourtApplicationIds().forEach(courtApplicationId ->
                    sendCommandDeleteHearingForCourtApplication(jsonEnvelope, hearingDeleted.getHearingId(), courtApplicationId)
            );
        }
    }

    private void sendCommandDeleteHearingForProsecutionCase(final JsonEnvelope jsonEnvelope, final UUID hearingId, final UUID prosecutionCaseId) {
        final JsonObjectBuilder deleteHearingCommandBuilder = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(PROSECUTION_CASE_ID, prosecutionCaseId.toString());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_DELETE_HEARING_FOR_PROSECUTION_CASE),
                deleteHearingCommandBuilder.build()));
    }

    private void sendCommandDeleteHearingForCourtApplication(final JsonEnvelope jsonEnvelope, final UUID hearingId, final UUID courtApplicationId) {
        final JsonObjectBuilder deleteHearingCommandBuilder = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(COURT_APPLICATION_ID, courtApplicationId.toString());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_DELETE_HEARING_FOR_COURT_APPLICATION),
                deleteHearingCommandBuilder.build()));
    }

    private void sendCommandDeleteHearing(final JsonEnvelope jsonEnvelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(HEARING_ID, jsonEnvelope.payloadAsJsonObject().getString(HEARING_ID));

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_DELETE_HEARING),
                jsonObjectBuilder.build()));
    }

    private void logEvent(final String event, final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} event received with payload {}", event, jsonEnvelope.payloadAsJsonObject());
        }
    }
}
