package uk.gov.moj.cpp.progression.command.handler.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsersGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersGroupService.class.getName());
    public static final String ORGANISATION_ID = "organisationId";
    public static final String ORGANISATION_NAME = "organisationName";
    public static final String ORGANISATION_TYPE = "organisationType";

    public static final String GROUPS = "groups";
    public static final String GROUP_ID = "groupId";
    public static final String GROUP_NAME = "groupName";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;
    @Inject
    private Enveloper enveloper;

    protected JsonObject getOrganisationDetailsForUser(final Envelope<?> envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new NullPointerException("User id Not Supplied for the UserGroups look up"));
        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder().add("userId", userId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        if (notFound(response)
                || (response.payloadAsJsonObject().getString(ORGANISATION_ID) == null)) {
            LOGGER.debug("Unable to retrieve Organisation for User {}", userId);
            throw new IllegalArgumentException(format("Missing Organisation for User %s", userId));
        }
        return response.payloadAsJsonObject();
    }

    protected JsonObject getUserGroupsDetailsForUser(final Envelope<?> envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new NullPointerException("User id Not Supplied for the UserGroups look up"));
        final JsonObject getUserGroupsForUserRequest = Json.createObjectBuilder().add("userId", userId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getUserGroupsForUserRequest)
                .withName("usersgroups.get-logged-in-user-groups").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        if (notFound(response)
                || (response.payloadAsJsonObject().getJsonArray(GROUPS) == null)
                || (response.payloadAsJsonObject().getJsonArray(GROUPS).isEmpty())) {
            LOGGER.debug("Unable to retrieve User Groups for User {}", userId);
            throw new IllegalArgumentException(format("User %s does not belong to any of the HMCTS groups", userId));
        }
        return response.payloadAsJsonObject();
    }

    public OrganisationDetails getUserOrgDetails(final Envelope<?> envelope) {
        final JsonObject org = getOrganisationDetailsForUser(envelope);
        return OrganisationDetails.of(fromString(org.getString(ORGANISATION_ID)),
                org.getString(ORGANISATION_NAME),
                org.getString(ORGANISATION_TYPE));
    }

    public List<UserGroupDetails> getUserGroupsForUser(final Envelope<?> envelope) {
        final JsonObject userGroups = getUserGroupsDetailsForUser(envelope);
        return userGroups.getJsonArray(GROUPS)
                .getValuesAs(JsonObject.class)
                .stream()
                .map(o -> new UserGroupDetails(fromString(o.getString(GROUP_ID)), o.getString(GROUP_NAME)))
                .collect(toList());
    }

    private static boolean notFound(JsonEnvelope response) {
        final JsonValue payload = response.payload();
        return payload == null
                || payload.equals(JsonValue.NULL);
    }
}
