package uk.gov.moj.cpp.progression.command.service;

import org.apache.commons.collections.CollectionUtils;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrganisationService {

    private static final String DEFENCE_ASSOCIATION_QUERY = "defence.query.associated-organisation";
    private static final String DEFENCE_ASSOCIATED_DEFENDANTS_QUERY = "defence.query.get-associated-defendants";
    private static final String ASSOCIATION = "association";


    public JsonObject getAssociatedOrganisation(final Envelope<?> envelope, final String defendantId, final Requester requester) {

        final JsonObject getUserGroupsForUserRequest = Json.createObjectBuilder().add("defendantId", defendantId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getUserGroupsForUserRequest)
                .withName(DEFENCE_ASSOCIATION_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload().getJsonObject(ASSOCIATION);
    }

    public final List<UUID> getAssociatedDefendants(final Envelope<?> envelope, final Requester requester) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException("User id Not Supplied for the UserGroups look up"));

        final JsonObject request = Json.createObjectBuilder().add("userId", userId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(request)
                .withName(DEFENCE_ASSOCIATED_DEFENDANTS_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        final JsonArray defendantIdsArray = response.payload().getJsonArray("defendantIds");
        final List<UUID> defendantIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(defendantIdsArray)) {
            for (int i = 0; i < defendantIdsArray.size(); i++) {
                defendantIds.add(UUID.fromString(defendantIdsArray.getString(i)));
            }
        }
        return defendantIds;
    }

}
