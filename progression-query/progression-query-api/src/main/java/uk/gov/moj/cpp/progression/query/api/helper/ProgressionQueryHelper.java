package uk.gov.moj.cpp.progression.query.api.helper;

import static java.util.UUID.fromString;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.api.vo.Permission;
import uk.gov.moj.cpp.progression.query.api.vo.UserOrganisationDetails;

import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class ProgressionQueryHelper {

    private static final String USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP = "User id Not Supplied for the UserGroups look up";

    private ProgressionQueryHelper() {

    }
    public static JsonObject removeProperty(final JsonObject origin, final String key){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            if (!entry.getKey().equals(key)){
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonObject value){
        final JsonObjectBuilder builder = buildJsonBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonArray value){
        final JsonObjectBuilder builder = buildJsonBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    private static JsonObjectBuilder buildJsonBuilder(final JsonObject origin) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public static boolean isPermitted(final JsonEnvelope queryEnvelope, final UserDetailsLoader userDetailsLoader, final Requester requester, final String defendantId) {
        final String userId = queryEnvelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final UserOrganisationDetails organisationDetailsForUser = userDetailsLoader.getOrganisationDetailsForUser(queryEnvelope, requester, userId);
        final List<Permission> permissions = userDetailsLoader.getPermissions(queryEnvelope.metadata(), requester, defendantId);
        if(permissions.isEmpty()) {
            return false;
        }

        final Permission organisationPermission = Permission.permission()
                .withTarget(fromString(defendantId))
                .withSource(organisationDetailsForUser.getOrganisationId())
                .build();

        final Permission userPermission = Permission.permission()
                .withTarget(fromString(defendantId))
                .withSource(fromString(userId))
                .build();

        return permissions.contains(organisationPermission) || permissions.contains(userPermission);
    }


}
