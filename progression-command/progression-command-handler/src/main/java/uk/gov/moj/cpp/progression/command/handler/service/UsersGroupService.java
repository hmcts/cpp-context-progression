package uk.gov.moj.cpp.progression.command.handler.service;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsersGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersGroupService.class.getName());
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String ORGANISATION_TYPE = "organisationType";
    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";

    private static final String GROUPS = "groups";
    private static final String GROUP_ID = "groupId";
    private static final String GROUP_NAME = "groupName";
    private static final String USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP = "User id Not Supplied for the UserGroups look up";
    private static final String USER_ID = "userId";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;
    @Inject
    private Enveloper enveloper;



    protected JsonEnvelope getOrganisationDetailsForUser(final Envelope<?> envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        LOGGER.info("User Id from envelope :: {}", envelope.metadata().userId());
        final JsonObject getOrganisationForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        LOGGER.info("Payload from envelope :: {}", requestEnvelope.payload());
        final JsonEnvelope usersAndGroupsRequestEnvelope = envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload());
        return requester.requestAsAdmin(usersAndGroupsRequestEnvelope);
    }

    protected Envelope<UserDetails> getUserDetailsAsAdmin(final Envelope<?> envelope) {
        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final JsonObject getUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        return requester.requestAsAdmin(envelop(getUserRequest).withName("usersgroups.get-user-details").withMetadataFrom(envelope), UserDetails.class);
    }

    protected JsonObject getUserGroupsDetailsForUser(final Envelope<?> envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final JsonObject getUserGroupsForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getUserGroupsForUserRequest)
                .withName("usersgroups.get-logged-in-user-groups").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        checkGroupExistsForUser(userId, response);
        return response.payloadAsJsonObject();
    }

    protected JsonEnvelope getOrganisationForLaaContractNumber(final Envelope<?> envelope, final String laaContractNumber) {

        final JsonObject getOrganisationRequest = Json.createObjectBuilder().add(LAA_CONTRACT_NUMBER, laaContractNumber).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getOrganisationRequest)
                .withName("usersgroups.get-organisation-details-by-laaContractNumber").withMetadataFrom(envelope);
        final JsonEnvelope organisationRequestEnvelope = JsonEnvelope.envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload());
        return requester.requestAsAdmin(organisationRequestEnvelope);
    }

    private void checkGroupExistsForUser(final String userId, final JsonEnvelope response) {
        if (notFound(response)
                || (response.payloadAsJsonObject().getJsonArray(GROUPS) == null)
                || (response.payloadAsJsonObject().getJsonArray(GROUPS).isEmpty())) {
            LOGGER.debug("Unable to retrieve User Groups for User {}", userId);
            throw new IllegalArgumentException(format("User %s does not belong to any of the HMCTS groups", userId));
        }
    }

    public Optional<UserDetails> getUserDetails(final Envelope<?> envelope) {
        final Envelope<UserDetails> response = getUserDetailsAsAdmin(envelope);
        return ofNullable(response.payload());
    }

    public OrganisationDetails getUserOrgDetails(final Envelope<?> envelope) {
        final JsonEnvelope orgResponse = getOrganisationDetailsForUser(envelope);
        if (notFound(orgResponse)) {
            return OrganisationDetails.newBuilder().build();
        }
        return OrganisationDetails.of(fromString(orgResponse.payloadAsJsonObject().getString(ORGANISATION_ID)),
                orgResponse.payloadAsJsonObject().getString(ORGANISATION_NAME),
                orgResponse.payloadAsJsonObject().getString(ORGANISATION_TYPE));
    }

    public OrganisationDetails getOrganisationDetailsForLAAContractNumber(final Envelope<?> envelope, final String laaContractNumber) {
        final JsonEnvelope orgResponse = getOrganisationForLaaContractNumber(envelope, laaContractNumber);
        if (emptyPayload(orgResponse)) {
            return OrganisationDetails.newBuilder().build();
        }
        return OrganisationDetails.of(fromString(orgResponse.payloadAsJsonObject().getString(ORGANISATION_ID)),
                orgResponse.payloadAsJsonObject().getString(ORGANISATION_NAME),
                orgResponse.payloadAsJsonObject().getString(ORGANISATION_TYPE));

    }

    public JsonObject getOrganisationDetailWithAddress(final Envelope<?> envelope) {
        final JsonEnvelope orgResponse = getOrganisationDetailsForUser(envelope);
        if (notFound(orgResponse)) {
            return null;
        }
        return orgResponse.payloadAsJsonObject();


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
                || payload.equals(JsonValue.NULL) ;
    }

    private static boolean emptyPayload(JsonEnvelope response) {
        final JsonObject payload = response.payloadAsJsonObject();

        return payload.isEmpty();
    }
}
