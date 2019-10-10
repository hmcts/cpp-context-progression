package uk.gov.moj.cpp.progression.query.api;

import static javax.json.Json.createObjectBuilder;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class UserDetailsLoader {
    public static final String GET_USER_DETAILS_REQUEST_ID = "usersgroups.get-user-details";
    public static final String USER_ID_PARAM = "userId";
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

}

