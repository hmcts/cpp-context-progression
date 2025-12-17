package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.Optional;

public class DefenceService {

    public Optional<JsonObject> getIdpcDetailsForDefendant(final Requester requester, final JsonEnvelope envelope, final String defendantId) {
        final JsonObject request = createObjectBuilder().add("defendantId", defendantId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(request)
                .withName("defence.query.defendant-idpc-metadata").withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return ofNullable(response.payload());
    }

}
