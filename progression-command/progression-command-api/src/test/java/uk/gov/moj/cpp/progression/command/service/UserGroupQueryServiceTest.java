package uk.gov.moj.cpp.progression.command.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserGroupQueryServiceTest {
    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Requester requester;

    @InjectMocks
    private UserGroupQueryService userGroupQueryService;

    private UUID userId = UUID.randomUUID();

    @Mock
    private JsonEnvelope responseJsonEnvelope;

    @Before
    public void setup() {
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
    }

    @Test
    public void shouldReturnFalseWhenQueryResponseIsNull() {

        JsonEnvelope jsonEnvelope = buildJsonEnvelope();
        when(requester.request(any(JsonEnvelope.class), any(Class.class))).thenReturn(jsonEnvelope);
        assertThat(userGroupQueryService.doesUserBelongsToHmctsOrganisation(jsonEnvelope, userId), is(false));
    }

    @Test
    public void shouldReturnTrueeWhenResponseWithMatchingOrgId() {

        when(requester.request(any(JsonEnvelope.class), any(Class.class))).thenReturn(buildOrgJsonEnvelope());
        assertThat(userGroupQueryService.doesUserBelongsToHmctsOrganisation(jsonEnvelope, userId), is(true));
    }

    @Test
    public void shouldReturnFalseWhenResponseWithNoMatchingOrgId() {
        when(requester.request(any(JsonEnvelope.class), any(Class.class))).thenReturn(buildOrgJsonEnvelope());
        assertThat(userGroupQueryService.doesUserBelongsToHmctsOrganisation(jsonEnvelope, userId), is(true));
    }



    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-logged-in-user-details")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

    public static JsonEnvelope buildJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-details").build(),
                createObjectBuilder().add("userId", "a085e359-6069-4694-8820-7810e7dfe762").build());
    }

    public static JsonEnvelope buildOrgJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-details").build(),
                createObjectBuilder().add("userId", "a085e359-6069-4694-8820-7810e7dfe762")
                        .add("organisationId", "1371dfe8-8aa5-47f7-bb76-275b83fc312d").build());
    }

    public static JsonEnvelope buildNoOrgJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("usersgroups.get-logged-in-user-details").build(),
                createObjectBuilder().add("userId", "a085e359-6069-4694-8820-7810e7dfe762")
                        .add("organisationId", "2471dfe8-8aa5-47f7-bb76-275b83fc312d").build());
    }
}