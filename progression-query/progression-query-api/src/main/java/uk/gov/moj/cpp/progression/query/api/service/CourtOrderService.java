package uk.gov.moj.cpp.progression.query.api.service;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class CourtOrderService {

    private static final String COURT_ORDERS_QUERY = "applicationscourtorders.query.court-order-by-defendant-id";

    public JsonObject getCourtOrdersByDefendant(final Envelope<?> envelope, final UUID defendantId, final Requester requester) {

        final JsonObject request = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(request)
                .withName(COURT_ORDERS_QUERY)
                .withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        return response.payload();
    }
}
