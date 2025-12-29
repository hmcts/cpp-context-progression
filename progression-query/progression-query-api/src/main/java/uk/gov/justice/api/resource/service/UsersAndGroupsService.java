package uk.gov.justice.api.resource.service;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.query.view.UserGroupsDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class UsersAndGroupsService {

    public static final String GROUPS = "groups";
    private static final String QUERY_GROUPS_FOR_USER = "usersgroups.get-groups-by-user";
    private static final String USER_ID = "userId";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonObject getOrganisationDetails(final JsonEnvelope envelope) {

        final JsonObject organisationDetail = JsonObjects.createObjectBuilder().add("organisationId",
                envelope.payloadAsJsonObject().getJsonString("organisationId").getString()).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(organisationDetail)
                .withName("usersgroups.get-organisation-details").withMetadataFrom(envelope);
        final JsonEnvelope usersAndGroupsRequestEnvelope = JsonEnvelope.envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload());
        final JsonEnvelope response = requester.requestAsAdmin(usersAndGroupsRequestEnvelope);
        return response.payloadAsJsonObject();
    }

    public List<UserGroupsDetails> getUserGroups(final UUID userId) {
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(QUERY_GROUPS_FOR_USER);
        final Envelope<JsonValue> requestEnvelope = envelopeFrom(metadataBuilder, createObjectBuilder().add(USER_ID, userId.toString()).build());
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        if (response.payload() == JsonValue.NULL || !response.payload().containsKey(GROUPS)) {
            return new ArrayList<>();
        }

        return response.payload().getJsonArray(GROUPS)
                .stream().map(jv -> (JsonObject) jv)
                .map(usrGrpJson -> new UserGroupsDetails(fromString(usrGrpJson.getString("groupId")), usrGrpJson.getString("groupName")))
                .collect(Collectors.toList());
    }
}
