package uk.gov.moj.cpp.progression.service;

import static java.lang.String.join;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.payloads.UserGroupDetails;
import uk.gov.moj.cpp.progression.service.pojo.UsersGroupGroup;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsersGroupService {
    private static final String USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP = "User id Not Supplied for the UserGroups look up";
    private static final String USER_ID = "userId";
    private static final String GROUPS = "groups";
    private static final String GROUP_ID = "groupId";
    private static final String GROUP_NAME = "groupName";
    private static final String EMAIL = "email";
    private static final String ORGANISATION_ID = "organisationId";


    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    public Optional<DefenceOrganisationVO> getDefenceOrganisationDetails(final UUID organisationId, final Metadata metadata) {
        log.info("Getting defence organisation details for organisation id {} " + organisationId.toString());

        final JsonObject getOrganisationForUserRequest = JsonObjects.createObjectBuilder().add(ORGANISATION_ID, organisationId.toString()).build();
        final Metadata metadataWithActionName = metadataWithNewActionName(metadata, "usersgroups.get-organisation-details");
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final JsonEnvelope response = requester.requestAsAdmin(requestEnvelope);
        final JsonObject jsonObject = response.payloadAsJsonObject();

        log.info("Response organisation details returned : {}" + response.toObfuscatedDebugString());

        return Optional.of(DefenceOrganisationVO.builder()
                .name(jsonObject.getString("organisationName", null))
                .email(jsonObject.getString(EMAIL, null))
                .phoneNumber(jsonObject.getString("phoneNumber", null))
                .addressLine1(jsonObject.getString("addressLine1", null))
                .addressLine2(jsonObject.getString("addressLine2", null))
                .addressLine3(jsonObject.getString("addressLine3", null))
                .addressLine4(jsonObject.getString("addressLine4", null))
                .postcode(jsonObject.getString("addressPostcode", null))
                .build());
    }

    public String getGroupIdForDefenceLawyers() {
        final JsonObject getOrganisationForUserRequest = JsonObjects.createObjectBuilder().add(GROUP_NAME, "Defence Lawyers").build();
        final Metadata metadata = metadataBuilder().withName("usersgroups.get-group-details-byname")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();
        final Metadata metadataWithActionName = metadataWithNewActionName(metadata, "usersgroups.get-group-details-byname");
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final Envelope<UsersGroupGroup> response = requester.requestAsAdmin(requestEnvelope, UsersGroupGroup.class);
        return response.payload().getGroupId().toString();
    }

    public Map<String, String> getEmailsForOrganisationIds(final JsonEnvelope envelope, final List<String> orgIds) {
        final JsonObject orgIdsForEmails = createObjectBuilder().add("ids", join(",", orgIds)).build();
        final Envelope<JsonObject> requestEnvelope = envelop(orgIdsForEmails)
                .withName("usersgroups.get-organisations-details-forids").withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        final JsonArray organisations = response.payload().getJsonArray("organisations");
        final Stream<JsonObject> stream = organisations.getValuesAs(JsonObject.class).stream();
        return stream
                .filter(x -> x.containsKey(EMAIL))
                .collect(Collectors.toMap(json -> json.getString(ORGANISATION_ID), json -> json.getString(EMAIL)));
    }

    protected JsonObject getUserGroupsDetailsForUser(final JsonEnvelope envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(USER_ID_NOT_SUPPLIED_FOR_THE_USER_GROUPS_LOOK_UP));
        final JsonObject getUserGroupsForUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getUserGroupsForUserRequest)
                .withName("usersgroups.get-logged-in-user-groups").withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        return response.payload();
    }

    public List<UserGroupDetails> getUserGroupsForUser(final JsonEnvelope envelope) {
        final JsonObject userGroups = getUserGroupsDetailsForUser(envelope);
        return userGroups.getJsonArray(GROUPS)
                .getValuesAs(JsonObject.class)
                .stream()
                .map(o -> new UserGroupDetails(fromString(o.getString(GROUP_ID)), o.getString(GROUP_NAME)))
                .collect(toList());
    }

    public JsonObject getGroupsWithOrganisation(final JsonEnvelope event) {
        final JsonObject payload = JsonObjects.createObjectBuilder().build();

        final Envelope<JsonObject> response = requester.requestAsAdmin(envelop(payload)
                .withName("usersgroups.get-groups-with-organisation")
                .withMetadataFrom(event), JsonObject.class);

        return response.payload();
    }

    public JsonObject getUserById(final JsonEnvelope event, final String userId){
        final Envelope<JsonObject> jsonObjectEnvelope = requester.requestAsAdmin(envelop(createObjectBuilder()
                .add(USER_ID, userId).build())
                .withName("usersgroups.get-user-details").withMetadataFrom(event), JsonObject.class);
        return jsonObjectEnvelope.payload();
    }

    public boolean isUserPartOfGroup(final JsonEnvelope envelope, final String group) {
        boolean isUserPartOfGroup = false;
        final List<UserGroupDetails> userGroupDetailsList = getUserGroupsForUser(envelope);
        if (isNotEmpty(userGroupDetailsList)) {
            isUserPartOfGroup = userGroupDetailsList.stream()
                    .anyMatch(userGroupDetails -> group.contentEquals(userGroupDetails.getGroupName()));
        }
        return isUserPartOfGroup;
    }

    public UUID getOrganisationByType(final Metadata metadata) {
        final JsonObject getRequest = createObjectBuilder()
                .add("name", "HMCTS")
                .add("type", "HMCTS")
                .add("limit", 1)
                .build();
        final Envelope<JsonObject> response = requester.requestAsAdmin(Envelope.envelopeFrom(metadataFrom(metadata)
                .withName("usersgroups.organisations")
                .build(), getRequest), JsonObject.class);


        final String organisationIdStr = response.payload().getJsonArray("organisations")
                .getValuesAs(JsonObject.class)
                .stream().map(s -> s.getString(ORGANISATION_ID)).findFirst().orElseThrow(RuntimeException::new);
        return UUID.fromString(organisationIdStr);
    }

}




