package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toSet;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.adapter.rest.exception.ConflictedResourceException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits offences off an existing hearing onto a new one (ADR-031, Option 4 refined). Listing
 * proxies the court-calendar split here; progression owns the decision and reuses the two events it
 * already has, so nothing books courtscheduler under the source hearing id.
 *
 * <p>Validation compares against the <strong>viewstore</strong>, not {@code HearingAggregate}: the
 * aggregate never applies {@code HearingUpdatedForPartialAllocation}, so it cannot see offences that
 * earlier splits already removed and would accept a request for offences that are no longer listed.
 *
 * <p>The requested offences must be a <strong>strict</strong> subset. An equal set is not a split —
 * it would move everything and leave an empty hearing behind — so it is rejected rather than quietly
 * treated as a move.
 */
@ServiceComponent(COMMAND_API)
public class SplitHearingApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitHearingApi.class);

    private static final String HEARING_ID = "hearingId";
    private static final String LIST_NEW_HEARING = "listNewHearing";
    private static final String LIST_DEFENDANT_REQUESTS = "listDefendantRequests";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String DEFENDANT_OFFENCES = "defendantOffences";
    private static final String PROSECUTION_CASES_TO_REMOVE = "prosecutionCasesToRemove";
    private static final String HEARING_LISTING_STATUS = "hearingListingStatus";
    private static final String HEARING_RESULTED = "HEARING_RESULTED";

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Handles("progression.split-hearing")
    public void handle(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID sourceHearingId = sourceHearingId(payload);

        final JsonObject hearingResponse = loadHearingResponse(envelope, sourceHearingId);
        rejectIfResulted(hearingResponse, sourceHearingId);
        final JsonObject hearing = hearingResponse.getJsonObject("hearing");

        final Map<UUID, Map<UUID, Set<UUID>>> storedOffences = offencesOnHearing(hearing);
        final Map<UUID, Map<UUID, Set<UUID>>> requestedOffences = requestedOffences(payload);

        validateStrictSubset(storedOffences, requestedOffences, sourceHearingId);

        final JsonObject command = createObjectBuilder(payload)
                .add(HEARING_ID, sourceHearingId.toString())
                .add(PROSECUTION_CASES_TO_REMOVE, prosecutionCasesToRemove(requestedOffences))
                .build();

        sender.send(envelop(command)
                .withName("progression.command.split-hearing")
                .withMetadataFrom(envelope));
    }

    private UUID sourceHearingId(final JsonObject payload) {
        if (!payload.containsKey(HEARING_ID) || payload.isNull(HEARING_ID)) {
            throw new BadRequestException("hearingId is required");
        }
        try {
            return fromString(payload.getString(HEARING_ID));
        } catch (final IllegalArgumentException ex) {
            throw new BadRequestException("hearingId is not a valid UUID");
        }
    }

    private JsonObject loadHearingResponse(final JsonEnvelope envelope, final UUID hearingId) {
        final JsonEnvelope request = envelopeFrom(
                uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom(envelope.metadata())
                        .withName("progression.query.hearing"),
                createObjectBuilder().add(HEARING_ID, hearingId.toString()).build());

        final JsonEnvelope response = requester.request(request);
        return Optional.ofNullable(response)
                .map(JsonEnvelope::payloadAsJsonObject)
                .filter(body -> body.containsKey("hearing") && !body.isNull("hearing"))
                .orElseThrow(() -> new NotFoundException("No hearing found for hearingId " + hearingId));
    }

    /**
     * A resulted hearing is history: moving offences off it would rewrite a concluded record. The
     * status sits alongside the hearing in the query response, not inside it.
     */
    private void rejectIfResulted(final JsonObject hearingResponse, final UUID hearingId) {
        final boolean resulted = hearingResponse.containsKey(HEARING_LISTING_STATUS)
                && !hearingResponse.isNull(HEARING_LISTING_STATUS)
                && HEARING_RESULTED.equalsIgnoreCase(hearingResponse.getString(HEARING_LISTING_STATUS));
        if (resulted) {
            throw new ConflictedResourceException("Hearing is resulted and cannot be split", hearingId);
        }
    }

    private Map<UUID, Map<UUID, Set<UUID>>> offencesOnHearing(final JsonObject hearing) {
        final Map<UUID, Map<UUID, Set<UUID>>> byCase = new LinkedHashMap<>();
        if (!hearing.containsKey("prosecutionCases") || hearing.isNull("prosecutionCases")) {
            return byCase;
        }
        for (final JsonObject prosecutionCase : hearing.getJsonArray("prosecutionCases").getValuesAs(JsonObject.class)) {
            final UUID caseId = uuid(prosecutionCase, "id");
            if (caseId == null || !prosecutionCase.containsKey("defendants") || prosecutionCase.isNull("defendants")) {
                continue;
            }
            final Map<UUID, Set<UUID>> byDefendant = byCase.computeIfAbsent(caseId, key -> new LinkedHashMap<>());
            for (final JsonObject defendant : prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
                final UUID defendantId = uuid(defendant, "id");
                if (defendantId == null || !defendant.containsKey("offences") || defendant.isNull("offences")) {
                    continue;
                }
                final Set<UUID> offenceIds = byDefendant.computeIfAbsent(defendantId, key -> new LinkedHashSet<>());
                defendant.getJsonArray("offences").getValuesAs(JsonObject.class).stream()
                        .map(offence -> uuid(offence, "id"))
                        .filter(java.util.Objects::nonNull)
                        .forEach(offenceIds::add);
            }
        }
        return byCase;
    }

    private Map<UUID, Map<UUID, Set<UUID>>> requestedOffences(final JsonObject payload) {
        final Map<UUID, Map<UUID, Set<UUID>>> byCase = new LinkedHashMap<>();
        final JsonObject listNewHearing = payload.containsKey(LIST_NEW_HEARING) && !payload.isNull(LIST_NEW_HEARING)
                ? payload.getJsonObject(LIST_NEW_HEARING)
                : null;
        if (listNewHearing == null
                || !listNewHearing.containsKey(LIST_DEFENDANT_REQUESTS)
                || listNewHearing.isNull(LIST_DEFENDANT_REQUESTS)) {
            throw new BadRequestException("listNewHearing.listDefendantRequests is required");
        }

        for (final JsonObject request : listNewHearing.getJsonArray(LIST_DEFENDANT_REQUESTS).getValuesAs(JsonObject.class)) {
            final UUID caseId = uuid(request, PROSECUTION_CASE_ID);
            final UUID defendantId = uuid(request, DEFENDANT_ID);
            if (caseId == null || defendantId == null) {
                throw new BadRequestException("each listDefendantRequest needs prosecutionCaseId and defendantId");
            }
            final Set<UUID> offenceIds = byCase
                    .computeIfAbsent(caseId, key -> new LinkedHashMap<>())
                    .computeIfAbsent(defendantId, key -> new LinkedHashSet<>());
            if (request.containsKey(DEFENDANT_OFFENCES) && !request.isNull(DEFENDANT_OFFENCES)) {
                request.getJsonArray(DEFENDANT_OFFENCES).getValuesAs(javax.json.JsonString.class)
                        .forEach(offence -> offenceIds.add(fromString(offence.getString())));
            }
        }
        return byCase;
    }

    private void validateStrictSubset(final Map<UUID, Map<UUID, Set<UUID>>> stored,
                                      final Map<UUID, Map<UUID, Set<UUID>>> requested,
                                      final UUID hearingId) {
        final Set<UUID> storedIds = flatten(stored);
        final Set<UUID> requestedIds = flatten(requested);

        if (requestedIds.isEmpty()) {
            throw new BadRequestException("No offences requested for the split");
        }

        final Set<UUID> notOnHearing = new LinkedHashSet<>(requestedIds);
        notOnHearing.removeAll(storedIds);
        if (!notOnHearing.isEmpty()) {
            throw new BadRequestException(
                    "Offences are not on hearing " + hearingId + ": " + notOnHearing);
        }

        if (requestedIds.containsAll(storedIds)) {
            throw new BadRequestException(
                    "A split must leave offences behind; hearing " + hearingId + " would be emptied");
        }

        LOGGER.info("split-hearing for hearing {}: moving {} of {} offences",
                hearingId, requestedIds.size(), storedIds.size());
    }

    private static Set<UUID> flatten(final Map<UUID, Map<UUID, Set<UUID>>> byCase) {
        final Set<UUID> all = new LinkedHashSet<>();
        byCase.values().forEach(byDefendant -> byDefendant.values().forEach(all::addAll));
        return all;
    }

    private static JsonArray prosecutionCasesToRemove(final Map<UUID, Map<UUID, Set<UUID>>> requested) {
        final JsonArrayBuilder cases = javax.json.Json.createArrayBuilder();
        requested.forEach((caseId, byDefendant) -> {
            final JsonArrayBuilder defendants = javax.json.Json.createArrayBuilder();
            byDefendant.forEach((defendantId, offenceIds) -> {
                final JsonArrayBuilder offences = javax.json.Json.createArrayBuilder();
                offenceIds.forEach(offenceId ->
                        offences.add(javax.json.Json.createObjectBuilder().add("offenceId", offenceId.toString())));
                defendants.add(javax.json.Json.createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId.toString())
                        .add("offencesToRemove", offences));
            });
            cases.add(javax.json.Json.createObjectBuilder()
                    .add("caseId", caseId.toString())
                    .add("defendantsToRemove", defendants));
        });
        return cases.build();
    }

    private static UUID uuid(final JsonObject object, final String field) {
        if (!object.containsKey(field) || object.isNull(field)) {
            return null;
        }
        try {
            return fromString(object.getString(field));
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }
}
