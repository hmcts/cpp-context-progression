package uk.gov.moj.cpp.progression.command.service;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import javax.json.Json;
import javax.json.JsonObject;

public class OrganisationService {

    private static final String DEFENCE_ASSOCIATION_QUERY = "defence.query.associated-organisation";
    private static final String ASSOCIATION = "association";


    public JsonObject getAssociatedOrganisation(final Envelope<?> envelope, final String defendantId, final Requester requester) {

        final JsonObject getUserGroupsForUserRequest = Json.createObjectBuilder().add("defendantId", defendantId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getUserGroupsForUserRequest)
                .withName(DEFENCE_ASSOCIATION_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload().getJsonObject(ASSOCIATION);
    }
}
