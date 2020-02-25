package uk.gov.justice.api.resource;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.providers.Provider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@Provider
@ApplicationScoped
public class CourtDocumentCommandProvider {

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public Action getDocumentTypeId(final Action action) {

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder().add("courtDocumentId", action.envelope().payloadAsJsonObject().getString("courtDocumentId"));


        final Envelope<JsonObject> jsonObjectEnvelope = Enveloper.envelop(jsonObjectBuilder.build())
                .withName("progression.query.courtdocument")
                .withMetadataFrom(action.envelope());
        final JsonEnvelope response = requester.request(jsonObjectEnvelope);

        return new Action(response);
    }
}
