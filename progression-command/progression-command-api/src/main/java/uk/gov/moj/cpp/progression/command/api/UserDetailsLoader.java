package uk.gov.moj.cpp.progression.command.api;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.command.api.vo.Permission;
import uk.gov.moj.cpp.progression.command.api.vo.UserOrganisationDetails;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class UserDetailsLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsLoader.class.getName());
    private static final String USER_ID = "userId";
    public static final String PERMISSIONS = "permissions";

    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String OBJECT = "object";
    public static final String ACTION = "action";
    public static final String ORGANISATION_ID = "organisationId";
    public static final String ORGANISATION_NAME = "organisationName";
    public static final String USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP = "User id Not Supplied for the UserGroups look up";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String ACCESS_TO_STANDALONE_APPLICATION = "Access to Standalone Application";


    public static boolean isUserHasPermissionForApplicationTypeCode(final Metadata metadata, final Requester requester, final String applicationTypeCode) {
        final JsonObject getOrganisationForUserRequest = JsonObjects.createObjectBuilder()
                .add(ACTION, ACCESS_TO_STANDALONE_APPLICATION)
                .add(OBJECT, applicationTypeCode)
                .build();
        final MetadataBuilder metadataWithActionName = Envelope.metadataFrom(metadata).withName("usersgroups.is-logged-in-user-has-permission-for-object");

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);

        if (response.payload().isEmpty()) {
            return true;
        }
        return response.payload().getBoolean("hasPermission");

    }


    public List<Permission> getPermissions(final Metadata metadata, final Requester requester, String defendantId) {
        final JsonObject getOrganisationForUserRequest = JsonObjects.createObjectBuilder().add(ACTION, "Upload").add(OBJECT, "DefendantDocuments").add(TARGET, defendantId).build();
        final MetadataBuilder metadataWithActionName = Envelope.metadataFrom(metadata).withName("usersgroups.permissions");

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
                .map(p -> (JsonObject)p)
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
        final String uuidString = JsonObjects.getString( permission, attribute).orElse(null);
        if (nonNull(uuidString)) {
            return fromString(uuidString);
        } else {
            return null;
        }

    }


    protected UserOrganisationDetails getOrganisationDetailsForUser(final Envelope<?> envelope, final Requester requester, String userId) {


        final JsonObject getOrganisationForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        final JsonEnvelope usersAndGroupsRequestEnvelope = envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload());
        final Envelope<JsonObject> response = requester.requestAsAdmin(usersAndGroupsRequestEnvelope, JsonObject.class);
        final JsonObject organisationDetails = response.payload();
        if(nonNull(organisationDetails) && organisationDetails.containsKey(ORGANISATION_ID)) {
            return new UserOrganisationDetails(fromString(organisationDetails.getString(ORGANISATION_ID)),
                    organisationDetails.getString(ORGANISATION_NAME));
        }

        return new UserOrganisationDetails();
    }

    public boolean isPermitted(final JsonEnvelope query, final Requester requester) {
        final String userId = query.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final UserOrganisationDetails organisationDetailsForUser = getOrganisationDetailsForUser(query, requester, userId);
        final List<Permission> permissions = getPermissions(query.metadata(), requester, query.payloadAsJsonObject().getString(DEFENDANT_ID));
        if(permissions.isEmpty()) {
            return false;
        }

        final Permission organisationPermission = Permission.permission()
                .withTarget(fromString(query.payloadAsJsonObject().getString(DEFENDANT_ID)))
                .withSource(organisationDetailsForUser.getOrganisationId())
                .build();

        final Permission userPermission = Permission.permission()
                .withTarget(fromString(query.payloadAsJsonObject().getString(DEFENDANT_ID)))
                .withSource(fromString(userId))
                .build();

        return permissions.contains(organisationPermission) || permissions.contains(userPermission);
    }

    public boolean isDefenceClient(final JsonEnvelope envelope, final Requester requester) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final JsonObject getPermissionsForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getPermissionsForUserRequest)
                .withName("usersgroups.get-logged-in-user-permissions").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        return checkPermissionExistsForUser(userId, response);

    }

    private boolean checkPermissionExistsForUser(final String userId, final JsonEnvelope response) {
        if (notFound(response)
                || (response.payloadAsJsonObject().getJsonArray(PERMISSIONS) == null)
                || (response.payloadAsJsonObject().getJsonArray(PERMISSIONS).isEmpty())) {
            LOGGER.debug("Unable to retrieve User Permissions for User {}", userId);
            throw new IllegalArgumentException(format("User %s does not belong to any of the HMCTS groups", userId));
        }

        final JsonArray permissions = response.payloadAsJsonObject().getJsonArray(PERMISSIONS);
        final JsonObject permissionJson = permissions.getValuesAs(JsonObject.class).stream()
                .filter(permission -> "defence-access".equals(permission.getString(ACTION)))
                .findFirst().orElse(null);
        return nonNull(permissionJson);

    }

    private static boolean notFound(JsonEnvelope response) {
        final JsonValue payload = response.payload();

        return payload == null
                || payload.equals(JsonValue.NULL);
    }

}

