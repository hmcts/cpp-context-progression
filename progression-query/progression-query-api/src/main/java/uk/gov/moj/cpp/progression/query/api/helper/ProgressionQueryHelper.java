package uk.gov.moj.cpp.progression.query.api.helper;

import static java.util.UUID.fromString;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.view.Permission;
import uk.gov.moj.cpp.progression.query.view.UserOrganisationDetails;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class ProgressionQueryHelper {

    private static final String USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP = "User id Not Supplied for the UserGroups look up";

    private ProgressionQueryHelper() {

    }
    public static JsonObject removeProperty(final JsonObject origin, final String key){
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
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

    public static JsonObjectBuilder buildJsonBuilder(final JsonObject origin) {
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public static boolean isPermitted(final JsonEnvelope queryEnvelope, final UserDetailsLoader userDetailsLoader, final Requester requester, final String defendantIds) {
        final String userId = queryEnvelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final UserOrganisationDetails organisationDetailsForUser = userDetailsLoader.getOrganisationDetailsForUser(queryEnvelope, requester, userId);

        final List<UUID> defendantIdList = commaSeparatedUuidParam2UUIDs(defendantIds);
        final AtomicBoolean isPermitted = new AtomicBoolean(true);
        defendantIdList.forEach(defendantId -> {
            final List<Permission> permissions = userDetailsLoader.getPermissions(queryEnvelope.metadata(), requester, defendantId);
            if(permissions.isEmpty()) {
                isPermitted.set(false);
            }else{
                final Permission organisationPermission = Permission.permission()
                        .withTarget(defendantId)
                        .withSource(organisationDetailsForUser.getOrganisationId())
                        .build();

                final Permission userPermission = Permission.permission()
                        .withTarget(defendantId)
                        .withSource(fromString(userId))
                        .build();

                if(!(permissions.contains(organisationPermission) || permissions.contains(userPermission))){
                    isPermitted.set(false);
                }
            }
        });

        return isPermitted.get();
    }

    private static List<UUID> commaSeparatedUuidParam2UUIDs(final String strUuids) {
        return Stream.of(strUuids.split(","))
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }


}
