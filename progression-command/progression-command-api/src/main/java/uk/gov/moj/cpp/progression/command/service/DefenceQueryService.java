package uk.gov.moj.cpp.progression.command.service;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefenceQueryService {

    public static final String DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE = "advocate.query.role-in-case-by-caseid";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceQueryService.class);
    private static final String BOTH = "both";
    private static final String PROSECUTING = "prosecuting";
    private static final String IS_ADVOCATE_DEFENDING_OR_PROSECUTING = "isAdvocateDefendingOrProsecuting";
    public static final String CASE_ID = "caseId";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public boolean isUserProsecutingCase(final JsonEnvelope jsonEnvelope, final UUID caseId) {
        final JsonEnvelope roleInCaseEnvelope = queryRoleInCase(jsonEnvelope, caseId);

        return Optional.ofNullable(roleInCaseEnvelope)
                .map(JsonEnvelope::payloadAsJsonObject)
                .filter(json -> nonNull(json.get(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .map(json -> PROSECUTING.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING))
                        || BOTH.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .orElse(false);
    }

    private JsonEnvelope queryRoleInCase(final Envelope<?> envelope, final UUID caseId) {
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId.toString()).build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE)
                .build();

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadata, payload));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} received with payload {}", DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE, jsonEnvelope.toObfuscatedDebugString());
        }
        return jsonEnvelope;
    }

}
