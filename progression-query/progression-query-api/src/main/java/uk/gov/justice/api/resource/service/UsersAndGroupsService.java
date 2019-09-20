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

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonObject getOrganisationDetailsForUser(final JsonEnvelope envelope) {

        final String userId = envelope.metadata().userId().orElseThrow(() -> new NullPointerException("User id Not Supplied for the UserGroups look up"));
        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder().add("userId", userId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        return response.payloadAsJsonObject();
    }
}
