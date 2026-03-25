package uk.gov.moj.cpp.progression.query.view.service;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefenceQueryService {
    public static final String DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE = "advocate.query.role-in-case-by-caseid";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceQueryService.class);
    private static final String DEFENDING = "defending";
    private static final String IS_ADVOCATE_DEFENDING_OR_PROSECUTING = "isAdvocateDefendingOrProsecuting";


    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public boolean isUserOnlyDefendingCase(final JsonEnvelope jsonEnvelope, final String caseId) {
        final JsonEnvelope roleInCaseEnvelope = queryRoleInCase(jsonEnvelope, caseId);

        if (isNull(roleInCaseEnvelope) || !JsonValue.ValueType.OBJECT.equals(roleInCaseEnvelope.payload().getValueType())) {
            return false;
        }

        return Optional.of(roleInCaseEnvelope)
                .map(JsonEnvelope::payloadAsJsonObject)
                .filter(json -> nonNull(json.get(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .map(json -> DEFENDING.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .orElse(false);
    }

    private JsonEnvelope queryRoleInCase(final Envelope<?> envelope, final String caseId) {
        final JsonObject payload = createObjectBuilder().add("caseId", caseId).build();

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
