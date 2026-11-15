package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Reads a case's SJP decisions from SJP ({@code sjp.query.case}) for Case at a Glance (CHD-3311).
 */
public class SjpQueryService {

    static final String SJP_QUERY_CASE = "sjp.query.case";

    private static final String CASE_DECISIONS = "caseDecisions";
    private static final String APPLICATION_DECISION = "applicationDecision";

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    /**
     * @return the case's decisions as SJP holds them, or an empty list if SJP has no such case or no decisions.
     */
    public List<SjpCaseDecision> getCaseDecisions(final JsonEnvelope envelope, final UUID caseId) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName(SJP_QUERY_CASE),
                createObjectBuilder().add("caseId", caseId.toString()).build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        if (isNull(response) || !(response.payload() instanceof JsonObject)) {
            return emptyList();
        }
        final JsonArray caseDecisions = response.payloadAsJsonObject().getJsonArray(CASE_DECISIONS);
        if (isNull(caseDecisions)) {
            return emptyList();
        }
        return caseDecisions.getValuesAs(JsonObject.class).stream()
                .map(SjpQueryService::toCaseDecision)
                .collect(toList());
    }

    private static SjpCaseDecision toCaseDecision(final JsonObject caseDecision) {
        return new SjpCaseDecision(
                UUID.fromString(caseDecision.getString("id")),
                ZonedDateTime.parse(caseDecision.getString("savedAt")),
                caseDecision.containsKey(APPLICATION_DECISION) && !caseDecision.isNull(APPLICATION_DECISION));
    }
}
