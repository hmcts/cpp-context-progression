package uk.gov.moj.cpp.progression.query.view;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDetailsLoader {
    public static final String GET_USER_DETAILS_REQUEST_ID = "usersgroups.get-user-details";
    private static final String QUERY_GROUPS_FOR_USER = "usersgroups.get-groups-by-user";
    private static final String USER_ID = "userId";
    private static final String GROUPS = "groups";
    private static final String USER_ID_PARAM = "userId";

    public static final String PERMISSIONS = "permissions";

    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String OBJECT = "object";
    public static final String ACTION = "action";
    public static final String ORGANISATION_ID = "organisationId";
    public static final String ORGANISATION_NAME = "organisationName";
    private static final String ACCESS_TO_STANDALONE_APPLICATION = "Access to Standalone Application";

    @ServiceComponent(Component.QUERY_VIEW)
    @Inject
    private Requester requester;

    public boolean isUserHasPermissionForApplicationTypeCode(final Metadata metadata, final String applicationTypeCode) {
        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder()
                .add(ACTION, ACCESS_TO_STANDALONE_APPLICATION)
                .add(OBJECT, applicationTypeCode)
                .build();
        final MetadataBuilder metadataWithActionName = Envelope.metadataFrom(metadata).withName("usersgroups.is-logged-in-user-has-permission-for-object");

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final Envelope<JsonObject> response = this.requester.request(requestEnvelope, JsonObject.class);

        if (response.payload().isEmpty()) {
            return true;
        }
        return response.payload().getBoolean("hasPermission");

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsLoader.class);

    public UserGroupsUserDetails getUserById(final Requester requester, final JsonEnvelope context, final UUID userId) {

        final Metadata metadata = metadataFrom(context.metadata()).withName(GET_USER_DETAILS_REQUEST_ID).build();
        final Envelope requestEnvelope = envelopeFrom(metadata, createObjectBuilder().add(USER_ID_PARAM, userId.toString()).build());
        final Envelope<JsonObject> jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope, JsonObject.class);


        final JsonObject userJson = jsonResultEnvelope.payload();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("mapped user %s to %s", userId, userJson));
        }
        final UserGroupsUserDetails result = new UserGroupsUserDetails();
        result.setUserId(userId);
        result.setFirstName(userJson.getString("firstName", null));
        result.setLastName(userJson.getString("lastName", null));
        return result;
    }

    public List<UserGroupsDetails> getGroupsUserBelongsTo(final Requester requester, final UUID userId) {
        LOGGER.info(" Calling {} to get groups that the user with the id {} belongs to ", QUERY_GROUPS_FOR_USER, userId);
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(QUERY_GROUPS_FOR_USER);
        final Envelope requestEnvelope = envelopeFrom(metadataBuilder, createObjectBuilder().add(USER_ID, userId.toString()).build());
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        if (response.payload() == JsonValue.NULL) {
            return new ArrayList<>();
        }
        final JsonArray jsonArray = response.payload().getJsonArray(GROUPS);
        final List<UserGroupsDetails> userGroupsDetails = new ArrayList<>();
        for (final JsonObject userGroup : jsonArray.getValuesAs(JsonObject.class)) {
            userGroupsDetails.add(new UserGroupsDetails(UUID.fromString(userGroup.getString("groupId")), userGroup.getString("groupName")));
        }
        return userGroupsDetails;
    }

    public List<Permission> getPermissions(final Metadata metadata, final Requester requester, final UUID defendantId) {
        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder().add(ACTION, "View").add(OBJECT, "DefendantDocuments").add(TARGET, defendantId.toString()).build();
        final MetadataBuilder metadataWithActionName = metadataFrom(metadata).withName("usersgroups.permissions");

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (!response.payload().containsKey(PERMISSIONS)) {
            return Collections.emptyList();
        }
        final JsonArray permissionsJsonArray = response.payload().getJsonArray(PERMISSIONS);

        if (permissionsJsonArray == null) {
            return Collections.emptyList();
        }

        return permissionsJsonArray.stream()
                .map(p -> (JsonObject) p)
                .map(permission ->
                        Permission.permission()
                                .withAction(JsonObjects.getString(permission, ACTION).orElse(null))
                                .withObject(JsonObjects.getString(permission, OBJECT).orElse(null))
                                .withSource(getNullableUUID(permission, SOURCE))
                                .withTarget(getNullableUUID(permission, TARGET))
                                .build()
                ).collect(Collectors.toList());
    }

    private static UUID getNullableUUID(final JsonObject permission, final String attribute) {
        final String uuidString = JsonObjects.getString(permission, attribute).orElse(null);
        if (nonNull(uuidString)) {
            return fromString(uuidString);
        } else {
            return null;
        }

    }


    public UserOrganisationDetails getOrganisationDetailsForUser(final Envelope<?> envelope, final Requester requester, String userId) {


        final JsonObject getOrganisationForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        final JsonEnvelope usersAndGroupsRequestEnvelope = envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload());
        final Envelope<JsonObject> response = requester.requestAsAdmin(usersAndGroupsRequestEnvelope, JsonObject.class);
        final JsonObject organisationDetails = response.payload();
        if (nonNull(organisationDetails) && organisationDetails.containsKey(ORGANISATION_ID)) {
            return new UserOrganisationDetails(fromString(organisationDetails.getString(ORGANISATION_ID)),
                    organisationDetails.getString(ORGANISATION_NAME));
        }

        return new UserOrganisationDetails();
    }


}

