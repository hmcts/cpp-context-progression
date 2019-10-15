package uk.gov.justice.api.resource.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class UsersAndGroupsService {

    public static final String GROUPS = "groups";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonObject getOrganisationDetails(final JsonEnvelope envelope) {

        final JsonObject organisationDetail = Json.createObjectBuilder().add("organisationId",
                envelope.payloadAsJsonObject().getJsonString("organisationId").getString()).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(organisationDetail)
                .withName("usersgroups.get-organisation-details").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        return response.payloadAsJsonObject();
    }
}
