package uk.gov.moj.cpp.progression.command.handler.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class UsersGroupServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private Requester requester;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Metadata metadata;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @InjectMocks
    private UsersGroupService usersGroupService;

    @Test
    public void shouldReturnOrganisationDetails() {

        //Given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        final JsonEnvelope response = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(organisationId.toString(), "organisationId").build();
        when(requester.requestAsAdmin(any())).thenReturn(response);

        //When
        final JsonEnvelope result = usersGroupService.getOrganisationDetailsForUser(query);

        //Then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(result.payloadAsJsonObject().getString("organisationId"), is(organisationId.toString()));

    }

    @Test
    public void shouldReturnUserGroups() {

        //Given
        final UUID userId = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        JsonObject userGroupsResponse = getHMCTSGroups();
        final JsonEnvelope response = envelopeFrom(metadataBuilder.build(), userGroupsResponse);
        when(requester.request(any())).thenReturn(response);

        //When
        List<UserGroupDetails> userGroupDetails = usersGroupService.getUserGroupsForUser(query);

        //Then
        verify(requester).request(envelopeArgumentCaptor.capture());
        assertEquals(userGroupsResponse.getJsonArray("groups").size(),
                userGroupDetails.size());
        assertEquals(userGroupsResponse.getJsonArray("groups").getJsonObject(0).getJsonString("groupId").getString(),
                userGroupDetails.get(0).getGroupId().toString());
        assertEquals(userGroupsResponse.getJsonArray("groups").getJsonObject(1).getJsonString("groupName").getString(),
                userGroupDetails.get(1).getGroupName().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldHandleNoUserGroups() {

        //Given
        final UUID userId = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        JsonObject userGroupsResponse = getNoGroups();
        final JsonEnvelope response = envelopeFrom(metadataBuilder.build(), userGroupsResponse);
        when(requester.request(any())).thenReturn(response);

        //When
        List<UserGroupDetails> userGroupDetails = usersGroupService.getUserGroupsForUser(query);

        //Then

    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionForMissingGroups() {

        final UUID userId = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        final JsonEnvelope response = envelopeFrom(
                metadataBuilder, JsonValue.NULL);
        when(requester.request(any())).thenReturn(response);
        usersGroupService.getUserGroupsForUser(query);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNullPointerExceptionForMissingUserId() {

        when(systemUserProvider.getContextSystemUserId()).thenReturn(null);
        final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, "userId").build();

        final JsonEnvelope response = envelopeFrom(metadataBuilder, JsonValue.NULL);
        when(requester.requestAsAdmin(any())).thenReturn(response);

        usersGroupService.getOrganisationDetailsForUser(query);

    }

    @Test(expected = NullPointerException.class)
    public void shouldNullPointerExceptionForMissingUserIdIngetUsersAndGroups() {

        when(systemUserProvider.getContextSystemUserId()).thenReturn(null);
        final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, "userId").build();

        final JsonEnvelope response = envelopeFrom(metadataBuilder, JsonValue.NULL);
        when(requester.request(any())).thenReturn(response);

        usersGroupService.getUserGroupsForUser(query);

    }

    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-organisation-details-for-user")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

    private MetadataBuilder getUserGroupDetailsMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-logged-in-user-groups")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

    private JsonObject getHMCTSGroups() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("groups", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("groupId", "7e2f143e-d619-40b3-8611-8015f3a18957")
                                .add("groupName", "Listing Officers")
                        )
                        .add(Json.createObjectBuilder()
                                .add("groupId", "8c5327b6-354e-4574-9558-b13fce8c055a")
                                .add("groupName", "Court Clerks")
                        )
                ).build();
        return payload;
    }

    private JsonObject getNoGroups() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("groups", Json.createArrayBuilder()).build();
        return payload;
    }

}