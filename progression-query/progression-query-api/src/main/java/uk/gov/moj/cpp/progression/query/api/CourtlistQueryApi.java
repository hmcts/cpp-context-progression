package uk.gov.moj.cpp.progression.query.api;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CourtlistQueryView;
import uk.gov.moj.cpp.progression.query.api.service.CourtlistQueryService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_API)
public class CourtlistQueryApi {

    @Inject
    private CourtlistQueryView courtlistQueryView;

    @Inject
    private CourtlistQueryService courtlistQueryService;

    @Handles("progression.search.court.list")
    public JsonEnvelope searchCourtlist(final JsonEnvelope query) {
        return courtlistQueryView.searchCourtlist(query);
    }

    @Handles("progression.search.prison.court.list")
    public JsonEnvelope searchPrisonCourtlist(final JsonEnvelope query) {
        return courtlistQueryView.searchPrisonCourtlist(query);
    }

    @Handles("progression.search.court.list.data")
    public JsonEnvelope searchCourtlistData(final JsonEnvelope query) {
        final JsonEnvelope queryWithApplications = ensureIncludeApplications(query);
        final JsonEnvelope respEnv = courtlistQueryView.searchCourtlist(queryWithApplications);
        return enrichCourtListJsonData(queryWithApplications, respEnv);
    }

    @Handles("progression.search.prison.court.list.data")
    public JsonEnvelope searchPrisonCourtlistData(final JsonEnvelope query) {
        final JsonEnvelope queryWithApplications = ensureIncludeApplications(query);
        final JsonEnvelope respEnv = courtlistQueryView.searchPrisonCourtlist(queryWithApplications);
        return enrichCourtListJsonData(queryWithApplications, respEnv);
    }

    private JsonEnvelope enrichCourtListJsonData(final JsonEnvelope query, final JsonEnvelope respEnv) {
        final JsonObject enrichedPayload = courtlistQueryService.buildEnrichedPayload(respEnv);
        return envelopeFrom(query.metadata(), enrichedPayload);
    }

    /**
     * Ensures includeApplications is in the payload for listing. Uses optional query param when present, otherwise defaults to true.
     */
    private static JsonEnvelope ensureIncludeApplications(final JsonEnvelope query) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JsonObject payload = query.payloadAsJsonObject();
        payload.keySet().forEach(key -> builder.add(key, payload.get(key)));
        final boolean includeApplications = payload.containsKey("includeApplications")
                ? payload.getBoolean("includeApplications")
                : false;
        builder.add("includeApplications", includeApplications);
        return envelopeFrom(query.metadata(), builder.build());
    }
}
