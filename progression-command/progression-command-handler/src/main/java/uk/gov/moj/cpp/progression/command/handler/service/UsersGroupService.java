package uk.gov.moj.cpp.progression.command.handler.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails.newBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsersGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersGroupService.class.getName());
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String ORGANISATION_TYPE = "organisationType";
    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";
    private static final String ADDRESS_1 = "addressLine1";
    private static final String ADDRESS_2 = "addressLine2";
    private static final String ADDRESS_3 = "addressLine3";
    private static final String ADDRESS_4 = "addressLine4";
    private static final String POSTCODE = "addressPostcode";
    private static final String EMAIL = "email";
    private static final String PHONE_NUMBER = "phoneNumber";

    private static final String GROUPS = "groups";
    private static final String GROUP_ID = "groupId";
    private static final String GROUP_NAME = "groupName";
    private static final String USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP = "User id Not Supplied for the UserGroups look up";
    private static final String USER_ID = "userId";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    private static boolean notFound(JsonEnvelope response) {
        final JsonValue payload = response.payload();

        return payload == null
                || payload.equals(JsonValue.NULL);
    }

    private static boolean emptyPayload(JsonObject response) {
        return isNull(response) || response.isEmpty();
    }

    public Envelope<JsonObject> getOrganisationDetailsForUser(final Envelope<?> envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        LOGGER.info("User Id from envelope :: {}", envelope.metadata().userId());
        final JsonObject getOrganisationForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        return requester.requestAsAdmin(envelopeFrom(
                metadataFrom(requestEnvelope.metadata()),
                requestEnvelope.payload()
                ), JsonObject.class
        );
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

    protected Envelope<JsonObject> getOrganisationForLaaContractNumber(final Envelope<?> envelope, final String laaContractNumber) {

        final JsonObject orgDetailsJsonEnvelope = Json.createObjectBuilder().add(LAA_CONTRACT_NUMBER, laaContractNumber).build();

        return requester.requestAsAdmin(envelopeFrom(
                metadataFrom(envelope.metadata()).withName("usersgroups.get-organisation-details-by-laaContractNumber"),
                orgDetailsJsonEnvelope
                ), JsonObject.class
        );
    }

    protected JsonObject getOrganisationForOrganisationId(final Envelope<?> envelope, final String organisationId) {

        final JsonObject orgDetailsJsonEnvelope = Json.createObjectBuilder().add(ORGANISATION_ID, organisationId).build();

        final Envelope<JsonObject> jsonResultEnvelope = requester.requestAsAdmin(envelopeFrom(
                metadataFrom(envelope.metadata()).withName("usersgroups.get-organisation-details"),
                orgDetailsJsonEnvelope
                ), JsonObject.class
        );

        if(isNull(jsonResultEnvelope) || emptyPayload(jsonResultEnvelope.payload())){
            return null;
        }

        return jsonResultEnvelope.payload();
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

    public OrganisationDetails getOrganisationDetailsForLAAContractNumber(final Envelope<?> envelope, final String laaContractNumber) {
        final Envelope<JsonObject> jsonResultEnvelope = getOrganisationForLaaContractNumber(envelope, laaContractNumber);

        if (isNull(jsonResultEnvelope) || emptyPayload(jsonResultEnvelope.payload())) {
            return newBuilder().build();
        }

        final JsonObject payload = jsonResultEnvelope.payload();
        return newBuilder()
                .withLaaContractNumber(payload.getString(LAA_CONTRACT_NUMBER, null))
                .withPhoneNumber(payload.getString(PHONE_NUMBER,null))
                .withName(payload.getString(ORGANISATION_NAME))
                .withAddressLine1(payload.getString(ADDRESS_1, null))
                .withAddressLine2(payload.getString(ADDRESS_2, null))
                .withAddressLine3(payload.getString(ADDRESS_3, null))
                .withAddressLine4(payload.getString(ADDRESS_4, null))
                .withEmail(payload.getString(EMAIL, null))
                .withId(fromString(payload.getString(ORGANISATION_ID)))
                .withType(payload.getString(ORGANISATION_TYPE))
                .withAddressPostcode(payload.getString(POSTCODE, null))
                .build();

    }

    public OrganisationDetails getOrganisationDetailsForOrganisationId(final Envelope<?> envelope, final String organisationId) {
        final JsonObject orgResponse = getOrganisationForOrganisationId(envelope, organisationId);
        if (emptyPayload(orgResponse)) {
            return newBuilder().build();
        }
        return newBuilder()
                .withAddressLine1(orgResponse.getString(ADDRESS_1))
                .withAddressLine2(getOptionalJsonString(orgResponse, ADDRESS_2))
                .withAddressLine3(getOptionalJsonString(orgResponse, ADDRESS_3))
                .withAddressLine4(orgResponse.getString(ADDRESS_4))
                .withAddressPostcode(orgResponse.getString(POSTCODE))
                .withEmail(orgResponse.getString(EMAIL))
                .withId(fromString(orgResponse.getString(ORGANISATION_ID)))
                .withLaaContractNumber(getOptionalJsonString(orgResponse, LAA_CONTRACT_NUMBER))
                .withName(orgResponse.getString(ORGANISATION_NAME))
                .withPhoneNumber(orgResponse.getString(ORGANISATION_NAME))
                .withType(orgResponse.getString(ORGANISATION_TYPE))
                .build();
    }

    private String getOptionalJsonString(final JsonObject jsonObject, final String fieldName) {
        return jsonObject.containsKey(fieldName) ? jsonObject.getString(fieldName) : null;
    }

    public List<UserGroupDetails> getUserGroupsForUser(final Envelope<?> envelope) {
        final JsonObject userGroups = getUserGroupsDetailsForUser(envelope);
        return userGroups.getJsonArray(GROUPS)
                .getValuesAs(JsonObject.class)
                .stream()
                .map(o -> new UserGroupDetails(fromString(o.getString(GROUP_ID)), o.getString(GROUP_NAME)))
                .collect(toList());
    }
}