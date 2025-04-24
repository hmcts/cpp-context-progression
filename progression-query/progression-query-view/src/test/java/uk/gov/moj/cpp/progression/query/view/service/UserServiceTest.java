package uk.gov.moj.cpp.progression.query.view.service;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private Requester requester;


    @Test
    void shouldGetUserGroupIdsByUserId() {
        final UUID group1 = UUID.randomUUID();
        final UUID group2 = UUID.randomUUID();
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "group1").add("groupId", group1.toString()).build())
                .add(Json.createObjectBuilder().add("groupName", "group2").add("groupId", group2.toString()).build())
                .build();
        final JsonObject groups = Json.createObjectBuilder().add("groups", userGroupArray).build();
        final Envelope envelope = Envelope.envelopeFrom(Envelope.metadataBuilder().
                withId(UUID.randomUUID()).
                withUserId(randomUUID().toString())
                .withName("test"), groups);
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withUserId(randomUUID().toString())
                        .withName("test").build(),
                Json.createObjectBuilder().build());
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenReturn(envelope);

        final List<UUID> groupIds = userService.getUserGroupIdsByUserId(jsonEnvelopeIn);

        assertThat(groupIds.size(), is(2));
        assertThat(groupIds.contains(group1), is(true));
        assertThat(groupIds.contains(group2), is(true));
    }

    @Test
    void shouldGetUserGroupsByUserId() {
        final String group1 = UUID.randomUUID().toString();
        final String group2 = UUID.randomUUID().toString();
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "group1").add("groupId", group1).build())
                .add(Json.createObjectBuilder().add("groupName", "group2").add("groupId", group2).build())
                .build();
        final JsonObject groups = Json.createObjectBuilder().add("groups", userGroupArray).build();
        final Envelope envelope = Envelope.envelopeFrom(Envelope.metadataBuilder().
                withId(UUID.randomUUID()).
                withUserId(randomUUID().toString())
                .withName("test"), groups);
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withUserId(randomUUID().toString())
                        .withName("test").build(),
                Json.createObjectBuilder().build());
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenReturn(envelope);

        final List<String> groupNames = userService.getUserGroupsByUserId(jsonEnvelopeIn);

        assertThat(groupNames.size(), is(2));
        assertThat(groupNames.contains("group1"), is(true));
        assertThat(groupNames.contains("group2"), is(true));
    }

}

