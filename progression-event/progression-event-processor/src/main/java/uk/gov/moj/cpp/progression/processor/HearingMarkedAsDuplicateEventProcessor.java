package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingMarkedAsDuplicateEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingMarkedAsDuplicateEventProcessor.class.getCanonicalName());
    private static final String HEARING_ID = "hearingId";
    private static final String PROSECUTION_CASE_IDS = "prosecutionCaseIds";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PRIVATE_HEARING_MARKED_AS_DUPLICATE_EVENT = "progression.event.hearing-marked-as-duplicate";
    private static final String COMMAND_MARK_HEARING_AS_DUPLICATE = "progression.command.mark-hearing-as-duplicate";
    private static final String COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE = "progression.command.mark-hearing-as-duplicate-for-case";



    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProgressionService progressionService;


    @Handles(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT)
    public void handleHearingMarkedAsDuplicatePublicEvent(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, envelope.toObfuscatedDebugString());
        }

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final String hearingId = envelope.payloadAsJsonObject().getString(HEARING_ID);
        jsonObjectBuilder.add(HEARING_ID, hearingId);

        if (envelope.payloadAsJsonObject().containsKey(PROSECUTION_CASE_IDS)) {
            jsonObjectBuilder.add(PROSECUTION_CASE_IDS, envelope.payloadAsJsonObject().getJsonArray(PROSECUTION_CASE_IDS));
        }

        if (envelope.payloadAsJsonObject().containsKey(DEFENDANT_IDS)) {
            jsonObjectBuilder.add(DEFENDANT_IDS, envelope.payloadAsJsonObject().getJsonArray(DEFENDANT_IDS));
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE),
                jsonObjectBuilder.build()));

        progressionService.populateHearingToProbationCaseworker(envelope, fromString(hearingId));

    }

    @Handles(PRIVATE_HEARING_MARKED_AS_DUPLICATE_EVENT)
    public void handleHearingMarkedAsDuplicatePrivateEvent(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_HEARING_MARKED_AS_DUPLICATE_EVENT, envelope.toObfuscatedDebugString());
        }

        final JsonObject payload = envelope.payloadAsJsonObject();
        final HearingMarkedAsDuplicate hearingMarkedAsDuplicate = jsonObjectToObjectConverter.convert(payload, HearingMarkedAsDuplicate.class);

        if (isNotEmpty(hearingMarkedAsDuplicate.getCaseIds())) {
            hearingMarkedAsDuplicate.getCaseIds().forEach(caseId ->
                    sendCommandMarkHearingAsDuplicateForCase(envelope, hearingMarkedAsDuplicate.getHearingId(), caseId, hearingMarkedAsDuplicate.getDefendantIds())
            );
        }
    }

    private void sendCommandMarkHearingAsDuplicateForCase(final JsonEnvelope envelope, final UUID hearingId, final UUID caseId, final List<UUID> defendantIds) {

        final JsonObjectBuilder markHearingCommandBuilder = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(CASE_ID, caseId.toString());

        if (CollectionUtils.isNotEmpty(defendantIds)) {
            final JsonArrayBuilder defendantBuilder = createArrayBuilder();
            defendantIds.forEach(defendantId -> defendantBuilder.add(defendantId.toString()));
            markHearingCommandBuilder.add(DEFENDANT_IDS, defendantBuilder.build());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE),
                markHearingCommandBuilder.build()));
    }

}
