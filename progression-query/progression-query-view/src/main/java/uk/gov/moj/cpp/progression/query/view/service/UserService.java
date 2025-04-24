package uk.gov.moj.cpp.progression.query.view.service;


import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.accesscontrol.drools.Action;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class UserService {
    private static final String GROUPS = "groups";
    private static final String GROUP_NAME = "groupName";
    private static final String GROUP_ID = "groupId";

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    public List<UUID> getUserGroupIdsByUserId(final JsonEnvelope envelope) {
        return getUserGroupsByUserId(new Action(envelope)).orElseThrow(() -> new IllegalStateException("no user group is defined for user"))
                .getJsonArray(GROUPS).stream()
                .map(JsonObject.class::cast)
                .map(gr -> gr.getString(GROUP_ID))
                .map(UUID::fromString)
                .toList();
    }

    public List<String> getUserGroupsByUserId(final JsonEnvelope envelope) {
        return getUserGroupsByUserId(new Action(envelope)).orElseThrow(() -> new IllegalStateException("no user group is defined for user"))
                .getJsonArray(GROUPS).stream()
                .map(JsonObject.class::cast)
                .map(gr -> gr.getString(GROUP_NAME))
                .toList();
    }

    private Optional<JsonObject> getUserGroupsByUserId(final Action action) {
        JsonObject userGroups = null;
        final Optional<String> userId = action.userId();
        if (userId.isPresent()) {
            final Metadata metadata = metadataFrom(action.envelope().metadata())
                    .withName("usersgroups.get-groups-by-user").build();
            final JsonObject payload =
                    Json.createObjectBuilder().add("userId", userId.get()).build();
            final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);

            final Envelope<JsonObject> response =
                    requester.requestAsAdmin(jsonEnvelope, JsonObject.class);
            if (response.payload().getValueType() != JsonValue.ValueType.NULL) {
                userGroups = response.payload();
            }
        }

        return Optional.ofNullable(userGroups);
    }
}

