package uk.gov.moj.cpp.progression.command.handler.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

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

    private static final String USER_ID = "userId";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String ORGANISATION_TYPE = "organisationType";
    private static final String LEGAL_ORGANISATION = "LEGAL_ORGANISATION";
    private static final String ORGANISATION_DISPLAY_NAME = "Greg Associates Ltd.";
    private static final String ADDRESS_LINE_1 = "Legal House";
    private static final String PHONE_NUMBER = "080012345678";

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
        JsonObject responseJsonObject = createObjectBuilder().add(ORGANISATION_ID, organisationId.toString()).build();
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(getMetadataBuilder(userId)).withPayloadOf(userId.toString(), USER_ID).build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject responsePayload = responseJsonObject;
            return envelopeFrom(envelope.metadata(), responsePayload);
        });


        //When
        final Envelope<JsonObject> result = usersGroupService.getOrganisationDetailsForUser(query);

        //Then
        verify(requester).requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class));
        assertThat(result.payload().getString(ORGANISATION_ID), is(organisationId.toString()));

    }

    @Test
    public void shouldReturnUserGroups() {

        //Given
        final UUID userId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), USER_ID).build();
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
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), USER_ID).build();
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
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), USER_ID).build();
        final JsonEnvelope response = envelopeFrom(
                metadataBuilder, JsonValue.NULL);
        when(requester.request(any())).thenReturn(response);
        assertThrows(IllegalArgumentException.class, () -> usersGroupService.getUserGroupsForUser(query));
    }

    @Test
    public void shouldNullPointerExceptionForMissingUserId() {

        assertThrows(NullPointerException.class, () -> {
            final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
            final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, USER_ID).build();

            final JsonEnvelope response = envelopeFrom(metadataBuilder, JsonValue.NULL);
            when(requester.requestAsAdmin(any())).thenReturn(response);
            usersGroupService.getOrganisationDetailsForUser(query);
        });
    }

    @Test
    public void shouldNullPointerExceptionForMissingUserIdIngetUsersAndGroups() {

        assertThrows(NullPointerException.class, () -> {
            final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
            final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, USER_ID).build();

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
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), USER_ID).build();
        final String laaContractNumber = "LAA1234";

        //When
        final OrganisationDetails result = usersGroupService.getOrganisationDetailsForLAAContractNumber(query, laaContractNumber);

        //Then
        verify(requester).requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class));
        assertEquals(null, result.getId());
        assertEquals(null, result.getName());
        assertEquals(null, result.getType());
    }


    /**
     * The response that jammed the queue. users-groups answered with a contract number and nothing
     * else, and the three fields read without a default - id, name and type - threw a
     * NullPointerException inside the command handler. That rolled the transaction back, Artemis
     * redelivered the same message, and the cycle repeated: the command never completed and its
     * public event was never raised. Reaching the assertions at all is most of the point of this
     * test.
     */
    @Test
    public void shouldReportTheOrganisationAsNotFoundWhenUsersGroupsAnswersWithoutIdNameOrType() {
        final UUID userId = randomUUID();
        final String laaContractNumber = "LAA3456";
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(getMetadataBuilder(userId)).withPayloadOf(userId.toString(), USER_ID).build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            return envelopeFrom(envelope.metadata(), createObjectBuilder()
                    .add("laaContractNumber", laaContractNumber)
                    .add("addressLine1", ADDRESS_LINE_1)
                    .build());
        });

        final OrganisationDetails result = usersGroupService.getOrganisationDetailsForLAAContractNumber(query, laaContractNumber);

        assertThat(result.getId(), is(nullValue()));
        assertThat(result.getName(), is(nullValue()));
        assertThat(result.getType(), is(nullValue()));
        assertThat(result.getLaaContractNumber(), is(nullValue()));
        assertThat(result.getAddressLine1(), is(nullValue()));
    }

    @Test
    public void shouldMapEveryFieldWhenUsersGroupsAnswersWithACompleteOrganisation() {
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final String laaContractNumber = "LAA3456";
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(getMetadataBuilder(userId)).withPayloadOf(userId.toString(), USER_ID).build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            return envelopeFrom(envelope.metadata(), createObjectBuilder()
                    .add(ORGANISATION_ID, organisationId.toString())
                    .add(ORGANISATION_NAME, ORGANISATION_DISPLAY_NAME)
                    .add(ORGANISATION_TYPE, LEGAL_ORGANISATION)
                    .add("laaContractNumber", laaContractNumber)
                    .add("addressLine1", ADDRESS_LINE_1)
                    .add("addressLine2", "15 Sewell Street")
                    .add("addressLine3", "Hammersmith")
                    .add("addressLine4", "London")
                    .add("addressPostcode", "SE14 2AB")
                    .add("phoneNumber", PHONE_NUMBER)
                    .add("email", "joe@example.com")
                    .build());
        });

        final OrganisationDetails result = usersGroupService.getOrganisationDetailsForLAAContractNumber(query, laaContractNumber);

        assertThat(result.getId(), is(organisationId));
        assertThat(result.getName(), is(ORGANISATION_DISPLAY_NAME));
        assertThat(result.getType(), is(LEGAL_ORGANISATION));
        assertThat(result.getLaaContractNumber(), is(laaContractNumber));
        assertThat(result.getAddressLine1(), is(ADDRESS_LINE_1));
        assertThat(result.getAddressLine2(), is("15 Sewell Street"));
        assertThat(result.getAddressLine3(), is("Hammersmith"));
        assertThat(result.getAddressLine4(), is("London"));
        assertThat(result.getAddressPostcode(), is("SE14 2AB"));
        assertThat(result.getPhoneNumber(), is(PHONE_NUMBER));
        assertThat(result.getEmail(), is("joe@example.com"));
    }

    /**
     * The phone number used to be read from the organisationName field, so every organisation
     * looked up by id came back carrying its own name as its telephone number.
     */
    @Test
    public void shouldTakeThePhoneNumberFromThePhoneNumberFieldWhenLookingUpByOrganisationId() {
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(getMetadataBuilder(userId)).withPayloadOf(userId.toString(), USER_ID).build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            return envelopeFrom(envelope.metadata(), createObjectBuilder()
                    .add(ORGANISATION_ID, organisationId.toString())
                    .add(ORGANISATION_NAME, ORGANISATION_DISPLAY_NAME)
                    .add(ORGANISATION_TYPE, LEGAL_ORGANISATION)
                    .add("phoneNumber", PHONE_NUMBER)
                    .build());
        });

        final OrganisationDetails result = usersGroupService.getOrganisationDetailsForOrganisationId(query, organisationId.toString());

        assertThat(result.getPhoneNumber(), is(PHONE_NUMBER));
        assertThat(result.getName(), is(ORGANISATION_DISPLAY_NAME));
    }

    /**
     * The by-id lookup read addressLine1, addressLine4 and addressPostcode without a default too,
     * so an organisation recorded without a full address threw rather than being returned.
     */
    @Test
    public void shouldReturnTheOrganisationWhenLookedUpByIdWithoutAFullAddress() {
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(getMetadataBuilder(userId)).withPayloadOf(userId.toString(), USER_ID).build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            return envelopeFrom(envelope.metadata(), createObjectBuilder()
                    .add(ORGANISATION_ID, organisationId.toString())
                    .add(ORGANISATION_NAME, ORGANISATION_DISPLAY_NAME)
                    .add(ORGANISATION_TYPE, LEGAL_ORGANISATION)
                    .build());
        });

        final OrganisationDetails result = usersGroupService.getOrganisationDetailsForOrganisationId(query, organisationId.toString());

        assertThat(result.getId(), is(organisationId));
        assertThat(result.getName(), is(ORGANISATION_DISPLAY_NAME));
        assertThat(result.getAddressLine1(), is(nullValue()));
        assertThat(result.getAddressPostcode(), is(nullValue()));
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
        return createObjectBuilder()
                .add("groups", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("groupId", "7e2f143e-d619-40b3-8611-8015f3a18957")
                                .add("groupName", "Listing Officers")
                        )
                        .add(createObjectBuilder()
                                .add("groupId", "8c5327b6-354e-4574-9558-b13fce8c055a")
                                .add("groupName", "Court Clerks")
                        )
                ).build();
    }

    private JsonObject getNoGroups() {
        return createObjectBuilder()
                .add("groups", createArrayBuilder()).build();
    }

}