package uk.gov.moj.cpp.progression.command.helper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.command.api.vo.Permission.permission;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.api.vo.Permission;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChildApplicationPermissionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChildApplicationPermissionChecker.class);
    private static final String USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS = "usersgroups.get-logged-in-user-permissions";
    private static final String USER_ID = "userId";
    private static final String PERMISSIONS = "permissions";
    private static final String OBJECT = "object";
    private static final String ACTION = "action";
    private static final String DESCRIPTION = "description";
    private static final String CREATE_CHILD_APPLICATION = "Create Child Application";
    private static final String LINK = "Link";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    /**
     * Whether the logged-in user holds the "Create Child Application" / "Link" permission.
     */
    public boolean hasLinkCreateChildApplicationPermission(final Metadata metadata) {
        final Optional<String> userId = metadata.userId();
        if (userId.isEmpty()) {
            LOGGER.warn("No userId in metadata, unable to check '{}' permission", CREATE_CHILD_APPLICATION);
            return false;
        }

        return getLoggedInUserPermissions(metadata, userId.get()).stream()
                .anyMatch(permission -> CREATE_CHILD_APPLICATION.equals(permission.getObject()) && LINK.equals(permission.getAction()));
    }

    private List<Permission> getLoggedInUserPermissions(final Metadata metadata, final String userId) {
        final JsonObject payload = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> response = requester.request(
                envelopeFrom(metadataFrom(metadata).withName(USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS), payload), JsonObject.class);

        return ofNullable(response.payload())
                .filter(json -> json.containsKey(PERMISSIONS))
                .map(json -> json.getJsonArray(PERMISSIONS).getValuesAs(JsonObject.class).stream()
                        .map(permissionJson -> permission()
                                .withObject(permissionJson.getString(OBJECT, null))
                                .withAction(permissionJson.getString(ACTION, null))
                                .withDescription(permissionJson.getString(DESCRIPTION, null))
                                .build())
                        .toList())
                .orElse(emptyList());
    }
}
