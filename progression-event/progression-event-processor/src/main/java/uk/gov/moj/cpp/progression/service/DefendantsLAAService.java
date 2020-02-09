package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

public class DefendantsLAAService {

    private static final String DEFENDANTS_BY_LAACONTRACTNUMBER_QUERY = "progression.query.defendants-by-laacontractnumber";
    private static final String DEFENDANTS = "defendants";


    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonArray getDefendantsByLAAContractNumber(final JsonEnvelope envelope, final String laaContractNumber) {

        final JsonObject getDefendantsByLaaContractNumberRequest = Json.createObjectBuilder().add("laaContractNumber", laaContractNumber).build();
        final JsonEnvelope response = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, DEFENDANTS_BY_LAACONTRACTNUMBER_QUERY)
                .apply(getDefendantsByLaaContractNumberRequest));
        return response.payloadAsJsonObject().getJsonArray(DEFENDANTS);
    }
}
