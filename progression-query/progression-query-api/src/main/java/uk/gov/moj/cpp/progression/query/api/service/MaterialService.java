package uk.gov.moj.cpp.progression.query.api.service;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

public class MaterialService {
    public static final String STRUCTURED_FORM_ID = "structuredFormId";


    public JsonObject getPet(final Requester requester, final JsonEnvelope query, final String petId){
        final JsonEnvelope materialResponse = requester.request(envelop(createObjectBuilder()
                .add(STRUCTURED_FORM_ID, petId)
                .build())
                .withName("material.query.structured-form")
                .withMetadataFrom(query));

        return materialResponse.payloadAsJsonObject();
    }
}
