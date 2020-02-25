package uk.gov.moj.cpp.progression.query.api;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class UserDetailsLoader {
    public static final String GET_USER_DETAILS_REQUEST_ID = "usersgroups.get-user-details";
    private static final String QUERY_GROUPS_FOR_USER = "usersgroups.get-groups-by-user";
    private static final String USER_ID = "userId";
    private static final String GROUPS = "groups";
    private static final String USER_ID_PARAM = "userId";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsLoader.class);

    @Inject
    private Enveloper enveloper;

    public UserGroupsUserDetails getUserById(final Requester requester, final JsonEnvelope context, final UUID userId) {

        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(context, GET_USER_DETAILS_REQUEST_ID)
                .apply(createObjectBuilder().add(USER_ID_PARAM, userId.toString())
                        .build());
        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);

        final JsonObject userJson = jsonResultEnvelope.payloadAsJsonObject();
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
        final JsonEnvelope response = requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder().add(USER_ID, userId.toString())));
        if (response.payload() == JsonValue.NULL) {
            return new ArrayList<>();
        }
        final JsonArray jsonArray = response.payloadAsJsonObject().getJsonArray(GROUPS);
        final List<UserGroupsDetails> userGroupsDetails = new ArrayList<>();
        for (final JsonObject userGroup : jsonArray.getValuesAs(JsonObject.class)) {
            userGroupsDetails.add(new UserGroupsDetails(UUID.fromString(userGroup.getString("groupId")), userGroup.getString("groupName")));
        }
        return userGroupsDetails;
    }

}

