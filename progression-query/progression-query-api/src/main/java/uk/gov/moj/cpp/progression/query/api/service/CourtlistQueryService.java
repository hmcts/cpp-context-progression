package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Shared service for court list query: builds the query envelope and enriches the payload with court centre data.
 * The interceptor chain is invoked by the adapter resources (which have the component annotation).
 * Used by both /courtlist (PDF/Word + pubhub) and /courtlistdata (JSON only).
 */
@ApplicationScoped
public class CourtlistQueryService {

    @Inject
    private ReferenceDataService referenceDataService;

    /**
     * Builds the court list query envelope. The caller (adapter resource) must run it through the interceptor chain.
     */
    public JsonEnvelope buildCourtlistQueryEnvelope(final String courtCentreId, final String courtRoomId,
                                                   final String listId, final String startDate, final String endDate,
                                                   final boolean restricted, final UUID userId, final String courtListAction) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("listId", listId)
                .add("startDate", startDate)
                .add("endDate", endDate)
                .add("restricted", restricted);

        if (nonNull(courtRoomId)) {
            payloadBuilder.add("courtRoomId", courtRoomId);
        }

        MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(courtListAction);
        if (nonNull(userId)) {
            metadataBuilder = metadataBuilder.withUserId(userId.toString());
        }
        return envelopeFrom(metadataBuilder.build(), payloadBuilder.build());
    }

    /**
     * Builds a JSON payload from the document, enriched with court centre data (ouCode, courtId)
     * when available from reference data.
     */
    public JsonObject buildEnrichedPayload(final JsonEnvelope document) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        document.payloadAsJsonObject().keySet().forEach(key ->
                builder.add(key, document.payloadAsJsonObject().get(key)));

        final String courtCentreName = document.payloadAsJsonObject().getString("courtCentreName", null);
        if (courtCentreName != null) {
            referenceDataService.getCourtCenterDataByCourtName(document, courtCentreName)
                    .ifPresent(courtCentreData -> {
                        builder.add("ouCode", courtCentreData.getJsonString("oucode"));
                        builder.add("courtId", courtCentreData.getJsonString("id"));
                    });
        }

        return builder.build();
    }
}
