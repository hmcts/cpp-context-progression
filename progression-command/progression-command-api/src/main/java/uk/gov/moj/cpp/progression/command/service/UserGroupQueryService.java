package uk.gov.moj.cpp.progression.command.service;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;


import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

public class UserGroupQueryService {

    public static final String LOGGED_IN_USER_ORGANISATION = "usersgroups.get-logged-in-user-details";
    public static final String USER_ID = "userId";
    public static final String HMCTS_ORGANISATION = "HMCTS";
    public static final String ORGANISATION_ID = "organisationId";
    public static final String ORGANISATION_TYPE = "organisationType";

    private static final String GROUPS = "groups";
    private static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    private static final String GROUP_NAME = "groupName";
    private static final String ORGANISATION_MATCH = "OrganisationMatch";
    private static final String ORGANISATION_MIS_MATCH = "OrganisationMisMatch";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public boolean doesUserBelongsToHmctsOrganisation(final JsonEnvelope jsonEnvelope, final UUID userId) {

        final JsonObject payload = createObjectBuilder().add(USER_ID, userId.toString()).build();
        boolean organisationFlag = false;

        final Envelope<JsonObject> envelope = envelop(payload)
                .withName(LOGGED_IN_USER_ORGANISATION)
                .withMetadataFrom(jsonEnvelope);

        final Envelope<JsonObject> jsonObjectEnvelope = requester.request(envelope, JsonObject.class);
        final String associatedOrganisationId = jsonObjectEnvelope.payload().getString(ORGANISATION_ID, null);
        final JsonObject organisationOjbect = getOrganisationDetails(jsonEnvelope, associatedOrganisationId);
        if(nonNull(organisationOjbect) && organisationOjbect.containsKey(ORGANISATION_TYPE) && HMCTS_ORGANISATION.equals(organisationOjbect.getString(ORGANISATION_TYPE))) {
                organisationFlag = true;
        }
        return organisationFlag;
    }


    public Optional<String> validateNonCPSUserOrg(final JsonEnvelope envelope, final UUID userId, final String groupName, final String shortName) {
        final JsonObject userGroupPayload = getUserGroups(envelope.metadata(), userId);
        if (isNonCpsUserGroup(userGroupPayload, groupName)) {
            if (isNonCpsProsecutors(userGroupPayload, shortName)) {
                return Optional.of(ORGANISATION_MATCH);
            } else{
                return Optional.of(ORGANISATION_MIS_MATCH);
            }
        }
        return Optional.empty();
    }

    private JsonObject getUserGroups(final Metadata metadata, final UUID userId) {
        final JsonObject getGroupsForUserRequest = JsonObjects.createObjectBuilder().add(USER_ID, userId.toString()).build();
        final Metadata metadataWithActionName = metadataFrom(metadata).withName("usersgroups.get-logged-in-user-groups").build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getGroupsForUserRequest);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        return response.payload();
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

    private JsonObject getOrganisationDetails(final JsonEnvelope envelope, final String organisationId) {

        final JsonObject organisationDetail = createObjectBuilder().add(ORGANISATION_ID, organisationId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(organisationDetail)
                .withName("usersgroups.get-organisation-details").withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        return response.payload();
    }
}
