package uk.gov.moj.cpp.progression.command.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setup() {
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
    }

    @Test
    public void shouldReturnFalseWhenQueryResponseIsNull() {
        when(requester.request(any(), any(Class.class))).thenReturn(buildJsonEnvelope());
        assertThat(userGroupQueryService.doesUserBelongsToHmctsOrganisation(jsonEnvelope, userId), is(false));
    }

    @Test
    public void shouldReturnTrueeWhenResponseWithMatchingOrgId() {
        when(requester.request(any(), any())).thenReturn(buildOrganisationJsonEnvelope());
        assertThat(userGroupQueryService.doesUserBelongsToHmctsOrganisation(jsonEnvelope, userId), is(true));
    }

    @Test
    public void shouldReturnFalseWhenResponseWithNoMatchingOrgId() {
        when(requester.request(any(), any())).thenReturn(buildNoMatchingOrganisationJsonEnvelope());
        assertThat(userGroupQueryService.doesUserBelongsToHmctsOrganisation(jsonEnvelope, userId), is(false));
    }

    @Test
    public void shouldReturnTrueForValidateNonCPSUserOrg() {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("test");
        final Envelope envelope = Envelope.envelopeFrom(metadataBuilder.build(), getUserGroupsResponse());
        when(requester.request(any(), any())).thenReturn(envelope);

        final Optional<String> isNonCPSUserOrg = userGroupQueryService.validateNonCPSUserOrg(jsonEnvelope, randomUUID(), "Non CPS Prosecutors", "DVLA");

        assertThat(isNonCPSUserOrg.isPresent(), is(true));
        assertThat(isNonCPSUserOrg.get(), is("OrganisationMatch"));
    }

    @Test
    public void shouldReturnFalseForValidateNonCPSUserOrg() {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("test");
        final Envelope envelope = Envelope.envelopeFrom(metadataBuilder.build(), getUserGroupsResponse());
        when(requester.request(any(), any())).thenReturn(envelope);

        final Optional<String> isNonCPSUserOrg = userGroupQueryService.validateNonCPSUserOrg(jsonEnvelope, randomUUID(), "Non CPS Prosecutors", "DVLA1");

        assertThat(isNonCPSUserOrg.isPresent(), is(true));
        assertThat(isNonCPSUserOrg.get(), is("OrganisationMisMatch"));
    }

    private JsonObject getUserGroupsResponse() {
        final JsonArrayBuilder groupsArray = createArrayBuilder();
        groupsArray.add(createObjectBuilder().add("groupId", String.valueOf(randomUUID())).add("groupName", "Non CPS Prosecutors"));
        groupsArray.add(createObjectBuilder().add("groupId", String.valueOf(randomUUID())).add("groupName", "DVLA Prosectutors").add("prosecutingAuthority" , "DVLA"));
        return createObjectBuilder().add("groups", groupsArray).build();
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
                createObjectBuilder().add("userId", "a085e359-6069-4694-8820-7810e7dfe762")
                        .add("organisationId", "b2d57737-6163-4bb9-88cb-97b45090d29e").build());
    }

    public static Envelope buildOrganisationJsonEnvelope() {
        return Envelope.envelopeFrom(
                Envelope.metadataBuilder().withId(randomUUID()).withName("usersgroups.get-organisation-details").build(),
                createObjectBuilder().add("organisationId", "b2d57737-6163-4bb9-88cb-97b45090d29d")
                        .add("organisationType", "HMCTS").build());
    }

    public static JsonEnvelope buildEmptyOrganisationJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("usersgroups.get-organisation-details").build(),
                createObjectBuilder().add("organisationId", "b2d57737-6163-4bb9-88cb-97b45090d29d").build());
    }

    public static Envelope buildNoMatchingOrganisationJsonEnvelope() {
        return Envelope.envelopeFrom(
                Envelope.metadataBuilder().withId(randomUUID()).withName("usersgroups.get-organisation-details").build(),
                createObjectBuilder().add("organisationId", "b2d57737-6163-4bb9-88cb-97b45090d29d")
                        .add("organisationType", "NONHMCTS").build());
    }
}