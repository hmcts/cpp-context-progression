package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.view.Permission;
import uk.gov.moj.cpp.progression.query.view.UserOrganisationDetails;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;

import java.util.List;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserDetailsLoaderTest {

    public static final String JSON_PERMISSION_JSON = "json/permission.json";
    public static final String JSON_ORGANISATION_JSON = "json/organisation.json";
    public static final String USER_GROUPS_GET_PERMISSION = "usersgroups.permissions";

    @InjectMocks
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Test
    public void shouldReturnAllPermissionForDefendant() {
        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(JSON_PERMISSION_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION);
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        final List<Permission> permissions = userDetailsLoader.getPermissions(metadata, requester, randomUUID());
        assertThat(permissions.size(),is(2));
        Permission firstPermission = Permission.permission().withSource(fromString("4a18bec5-ab1a-410a-9889-885694356401"))
                .withTarget(fromString("faee972d-f9dd-43d3-9f41-8acc3b908d09")).build();
        Permission secondPermission = Permission.permission().withSource(fromString("4a18bec5-ab1a-410a-9889-885694356402"))
                .withTarget(fromString("faee972d-f9dd-43d3-9f41-8acc3b908d09")).build();
        Permission inValidPermission = Permission.permission().withSource(fromString("4a18bec5-ab1a-410a-9889-885694356403"))
                .withTarget(fromString("faee972d-f9dd-43d3-9f41-8acc3b908d09")).build();
        assertThat(permissions.contains(firstPermission), is(true));
        assertThat(permissions.contains(secondPermission), is(true));
        assertThat(permissions.contains(inValidPermission), is(false));
    }

    @Test
    public void shouldReturnValidOrganisationDetails() {

        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(JSON_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION);

        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        final UserOrganisationDetails organisationDetailsForUser = userDetailsLoader.getOrganisationDetailsForUser(envelope, requester, metadata.userId().get());
        assertThat(organisationDetailsForUser.getOrganisationId(), is(fromString("1fc69990-bf59-4c4a-9489-d766b9abde9a")));
    }


    @Test
    public void shouldNotReturnOrganisationDetails() {

        final JsonObject jsonObjectPayload = JsonObjects.createObjectBuilder().build();
        final Metadata metadata = QueryClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION);

        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        final UserOrganisationDetails organisationDetailsForUser = userDetailsLoader.getOrganisationDetailsForUser(envelope, requester, metadata.userId().get());
        assertThat(organisationDetailsForUser.getOrganisationId(), nullValue());
    }
}