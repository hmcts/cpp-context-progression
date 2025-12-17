package uk.gov.moj.cpp.progression.query.api.service;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class UsersGroupQueryService {

    @ServiceComponent(Component.QUERY_VIEW)
    @Inject
    private Requester requester;

    public static final String GROUPS = "groups";
    public static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    public static final String GROUP_NAME = "groupName";
    public static final String ORGANISATION_MATCH = "OrganisationMatch";
    public static final String ORGANISATION_MIS_MATCH = "OrganisationMisMatch";

    public JsonObject getUserGroups(final Metadata metadata, final UUID userId) {

        final JsonObject getGroupsForUserRequest = Json.createObjectBuilder().add("userId", userId.toString()).build();
        final Metadata metadataWithActionName = metadataFrom(metadata).withName("usersgroups.get-logged-in-user-groups").build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getGroupsForUserRequest);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        return response.payload();
    }

    public Optional<String> validateNonCPSUserOrg(final Metadata metadata, final UUID userId, final String groupName, final String shortName) {
        final JsonObject userGroupPayload = getUserGroups(metadata, userId);
        if (isNonCpsUserGroup(userGroupPayload, groupName)) {
            if (isNonCpsProsecutors(userGroupPayload, shortName)) {
                return Optional.of(ORGANISATION_MATCH);
            } else{
                return Optional.of(ORGANISATION_MIS_MATCH);
            }
        }
        return Optional.empty();
    }

    private Boolean isNonCpsUserGroup(final JsonObject userGroups, final String groupName) {
        final Stream<JsonObject> stream = userGroups.getJsonArray(GROUPS).getValuesAs(JsonObject.class).stream();
        return stream
                .filter(userGroup -> userGroup.containsKey(GROUP_NAME))
                .anyMatch(userGroup -> groupName.equals(userGroup.getString(GROUP_NAME)));
    }

    private Boolean isNonCpsProsecutors(final JsonObject userGroups, final String shortName) {
        final Stream<JsonObject> stream = userGroups.getJsonArray(GROUPS).getValuesAs(JsonObject.class).stream();
        return stream
                .filter(userGroup -> userGroup.containsKey(PROSECUTING_AUTHORITY))
                .anyMatch(userGroup -> shortName.equals(userGroup.getString(PROSECUTING_AUTHORITY)));
    }
}
