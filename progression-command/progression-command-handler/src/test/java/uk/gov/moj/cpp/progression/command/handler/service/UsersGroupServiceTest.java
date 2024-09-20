package uk.gov.moj.cpp.progression.command.handler.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UsersGroupServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private Requester requester;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Metadata metadata;
    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;
    @InjectMocks
    private UsersGroupService usersGroupService;

    @Test
    public void shouldReturnOrganisationDetails() {
        //Given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        JsonObject responseJsonObject = Json.createObjectBuilder().add("organisationId",organisationId.toString()).build();
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(getMetadataBuilder(userId)).withPayloadOf(userId.toString(), "userId").build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject responsePayload = responseJsonObject;
            return envelopeFrom(envelope.metadata(), responsePayload);
        });


        //When
        final Envelope<JsonObject> result = usersGroupService.getOrganisationDetailsForUser(query);

        //Then
        verify(requester).requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class));
        assertThat(result.payload().getString("organisationId"), is(organisationId.toString()));

    }

    @Test
    public void shouldReturnUserGroups() {

        //Given
        final UUID userId = randomUUID();
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

    @Test
    public void shouldHandleNoUserGroups() {
        //Given
        final UUID userId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        JsonObject userGroupsResponse = getNoGroups();
        final JsonEnvelope response = envelopeFrom(metadataBuilder.build(), userGroupsResponse);
        when(requester.request(any())).thenReturn(response);

        //When
        assertThrows(IllegalArgumentException.class, () -> usersGroupService.getUserGroupsForUser(query));

        //Then

    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForMissingGroups() {
        final UUID userId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        final JsonEnvelope response = envelopeFrom(
                metadataBuilder, JsonValue.NULL);
        when(requester.request(any())).thenReturn(response);
        assertThrows(IllegalArgumentException.class, () -> usersGroupService.getUserGroupsForUser(query));
    }

    @Test
    public void shouldNullPointerExceptionForMissingUserId() {

        assertThrows(NullPointerException.class, () -> {
            final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
            final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, "userId").build();

            final JsonEnvelope response = envelopeFrom(metadataBuilder, JsonValue.NULL);
            when(requester.requestAsAdmin(any())).thenReturn(response);
            usersGroupService.getOrganisationDetailsForUser(query);
        });
    }

    @Test
    public void shouldNullPointerExceptionForMissingUserIdIngetUsersAndGroups() {

        assertThrows(NullPointerException.class, () -> {
            final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
            final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, "userId").build();

            final JsonEnvelope response = envelopeFrom(metadataBuilder, JsonValue.NULL);
            when(requester.request(any())).thenReturn(response);
            usersGroupService.getUserGroupsForUser(query);
        });
    }

    @Test
    public void shouldReturnUserDetailsGivenAKnownUserId() {
        //Given
        final UUID userId = randomUUID();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-user-details").withUserId(userId.toString()),
                createObjectBuilder().build());

        final UserDetails userDetails = new UserDetails("Bob", "Marley");

        final Envelope<UserDetails> returnedValue = envelop(userDetails)
                .withName("usersgroups.get-user-details")
                .withMetadataFrom(requestEnvelope);

        when(requester.requestAsAdmin(any(), eq(UserDetails.class))).thenReturn(returnedValue);
        //When
        final Optional<UserDetails> result = usersGroupService.getUserDetails(requestEnvelope);

        //Then
        verify(requester).requestAsAdmin(envelopeCaptor.capture(), eq(UserDetails.class));
        assertThat(envelopeCaptor.getValue().metadata().name(), is("usersgroups.get-user-details"));
        assertThat(result, is(Optional.of(userDetails)));
    }

    @Test
    public void shouldReturnEmptyOrganisationWhenGotEmptyPayloadFromGetOrgarnisationByLAAContractNumberAPI() {
        //Given
        final UUID userId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        final String laaContractNumber = "LAA1234";

        //When
        final OrganisationDetails result = usersGroupService.getOrganisationDetailsForLAAContractNumber(query, laaContractNumber);

        //Then
        verify(requester).requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class));
        assertEquals(null, result.getId());
        assertEquals(null, result.getName());
        assertEquals(null, result.getType());
    }


    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-organisation-details-for-user")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID());

        if (userId != null) {
                metadataBuilder.withUserId(userId.toString());
        }

        return metadataBuilder;
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