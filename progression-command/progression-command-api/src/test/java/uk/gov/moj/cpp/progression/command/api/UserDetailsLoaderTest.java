package uk.gov.moj.cpp.progression.command.api;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.CommandClientTestBase;
import uk.gov.moj.cpp.progression.command.api.vo.Permission;
import uk.gov.moj.cpp.progression.command.api.vo.UserOrganisationDetails;
import uk.gov.moj.cpp.progression.test.FileUtil;

import java.util.List;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserDetailsLoaderTest {

    public static final String JSON_PERMISSION_JSON = "json/permission.json";
    public static final String JSON_ORGANISATION_JSON = "json/organisation.json";
    public static final String JSON_NO_PERMISSION_ORGANISATION_JSON = "json/organisation_no_permission.json";
    public static final String USER_GROUPS_GET_PERMISSION = "usersgroups.permissions";

    @InjectMocks
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestEnvelopeCaptor;

    @Test
    public void shouldReturnAllPermissionForDefendant() {
        final JsonObject jsonObjectPayload = CommandClientTestBase.readJson(JSON_PERMISSION_JSON, JsonObject.class);
        final List<Permission> permissions = getPermissions(jsonObjectPayload);
        assertThat(permissions.size(),is(2));
        Permission firstPermission = Permission.permission().withSource(fromString("4a18bec5-ab1a-410a-9889-885694356401"))
                .withTarget(fromString("faee972d-f9dd-43d3-9f41-8acc3b908d09")).build();
        Permission secondPermission = Permission.permission().withSource(fromString("1fc69990-bf59-4c4a-9489-d766b9abde9a"))
                .withTarget(fromString("faee972d-f9dd-43d3-9f41-8acc3b908d09")).build();
        Permission inValidPermission = Permission.permission().withSource(fromString("4a18bec5-ab1a-410a-9889-885694356403"))
                .withTarget(fromString("faee972d-f9dd-43d3-9f41-8acc3b908d09")).build();
        assertThat(permissions.contains(firstPermission), is(true));
        assertThat(permissions.contains(secondPermission), is(true));
        assertThat(permissions.contains(inValidPermission), is(false));
    }

    @Test
    public void shouldReturnEmptyPermissions() {
        final JsonObject jsonObjectPayload = createObjectBuilder().build();
        final List<Permission> permissions = getPermissions(jsonObjectPayload);
        assertThat(permissions.size(),is(0));
    }

    private List<Permission> getPermissions(final JsonObject jsonObjectPayload){
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        return userDetailsLoader.getPermissions(metadata, requester, randomUUID().toString());
    }

    @Test
    public void shouldReturnValidOrganisationDetails() {

        final JsonObject jsonObjectPayload = CommandClientTestBase.readJson(JSON_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());

        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        final UserOrganisationDetails organisationDetailsForUser = userDetailsLoader.getOrganisationDetailsForUser(envelope, requester, metadata.userId().get());
        assertThat(organisationDetailsForUser.getOrganisationId(), is(fromString("1fc69990-bf59-4c4a-9489-d766b9abde9a")));
    }


    @Test
    public void shouldNotReturnOrganisationDetails() {

        final JsonObject jsonObjectPayload = createObjectBuilder().build();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());

        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);
        final UserOrganisationDetails organisationDetailsForUser = userDetailsLoader.getOrganisationDetailsForUser(envelope, requester, metadata.userId().get());
        assertThat(organisationDetailsForUser.getOrganisationId(), nullValue());
    }


    @Test
    public void shouldPermitBasedOnAssociation() {


        final JsonObject jsonObjectPayload = CommandClientTestBase.readJson(JSON_PERMISSION_JSON, JsonObject.class);
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        final JsonObject jsonObjectOrganisationPayload = CommandClientTestBase.readJson(JSON_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadataOrganisation = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());

        final Envelope envelopeOrganisation = Envelope.envelopeFrom(metadataOrganisation, jsonObjectOrganisationPayload);

        when(requester.requestAsAdmin(any(), any())).thenAnswer(invocation -> {
            Object argument = invocation.getArguments()[0];
            if(argument.toString().contains("usersgroups.get-organisation-details-for-user")) {
                return envelopeOrganisation;
            }
            return envelope;
        });


        final JsonObject jsonAddCourtDocument = createObjectBuilder().add("defendantId", "faee972d-f9dd-43d3-9f41-8acc3b908d09").build();
        final Metadata metadataAddCourtDocument = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final JsonEnvelope envelopeAddCourtDoc = JsonEnvelope.envelopeFrom(metadataAddCourtDocument, jsonAddCourtDocument);

        assertThat(userDetailsLoader.isPermitted(envelopeAddCourtDoc, requester), is(true));
    }


    @Test
    public void shouldPermitBasedOnGrantee() {


        final JsonObject jsonObjectPayload = CommandClientTestBase.readJson(JSON_PERMISSION_JSON, JsonObject.class);
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        final JsonObject jsonObjectOrganisationPayload = CommandClientTestBase.readJson(JSON_NO_PERMISSION_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadataOrganisation = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());

        final Envelope envelopeOrganisation = Envelope.envelopeFrom(metadataOrganisation, jsonObjectOrganisationPayload);

        when(requester.requestAsAdmin(any(), any())).thenAnswer(invocation -> {
            Object argument = invocation.getArguments()[0];
            if(argument.toString().contains("usersgroups.get-organisation-details-for-user")) {
                return envelopeOrganisation;
            }
            return envelope;
        });


        final JsonObject jsonAddCourtDocument = createObjectBuilder().add("defendantId", "faee972d-f9dd-43d3-9f41-8acc3b908d09").build();
        final Metadata metadataAddCourtDocument = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, "4a18bec5-ab1a-410a-9889-885694356401");
        final JsonEnvelope envelopeAddCourtDoc = JsonEnvelope.envelopeFrom(metadataAddCourtDocument, jsonAddCourtDocument);

        assertThat(userDetailsLoader.isPermitted(envelopeAddCourtDoc, requester), is(true));
    }


    @Test
    public void shouldNotPermit() {


        final JsonObject jsonObjectPayload = CommandClientTestBase.readJson(JSON_PERMISSION_JSON, JsonObject.class);
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        final JsonObject jsonObjectOrganisationPayload = CommandClientTestBase.readJson(JSON_NO_PERMISSION_ORGANISATION_JSON, JsonObject.class);
        final Metadata metadataOrganisation = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());

        final Envelope envelopeOrganisation = Envelope.envelopeFrom(metadataOrganisation, jsonObjectOrganisationPayload);

        when(requester.requestAsAdmin(any(), any())).thenAnswer(invocation -> {
            Object argument = invocation.getArguments()[0];
            if(argument.toString().contains("usersgroups.get-organisation-details-for-user")) {
                return envelopeOrganisation;
            }
            return envelope;
        });


        final JsonObject jsonAddCourtDocument = createObjectBuilder().add("defendantId", "faee972d-f9dd-43d3-9f41-8acc3b908d09").build();
        final Metadata metadataAddCourtDocument = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final JsonEnvelope envelopeAddCourtDoc = JsonEnvelope.envelopeFrom(metadataAddCourtDocument, jsonAddCourtDocument);

        assertThat(userDetailsLoader.isPermitted(envelopeAddCourtDoc, requester), is(false));
    }


    @Test
    public void shouldPermitBasedOnGrantee_1() {
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(metadata, createObjectBuilder().build()));

        final JsonObject jsonObj = createObjectBuilder().add("cotrId", "faee972d-f9dd-43d3-9f41-8acc3b908d09").build();
        final Metadata metadata1 = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, "4a18bec5-ab1a-410a-9889-885694356401");
        final JsonEnvelope envelopeAddCourtDoc = JsonEnvelope.envelopeFrom(metadata1, jsonObj);

        assertThrows(IllegalArgumentException.class, () ->userDetailsLoader.isDefenceClient(envelopeAddCourtDoc, requester));
    }

    @Test
    public void shouldReturnFalseForIsUserHasPermissionForApplicationTypeCodeWhenUsersGroupsQueryReturnFalse() {

        final JsonObject responsePayload = FileUtil.jsonFromString(FileUtil
                .getPayload("json/permission-for-application-type.json")
                .replaceAll("%HAS_PERMISSION%", "false"));

        final Metadata metadata = CommandClientTestBase.metadataFor("usersgroups.is-logged-in-user-has-permission-for-object", randomUUID().toString());
        final String applicationTypeCode = "anyCode";

        final Envelope envelope = Envelope.envelopeFrom(metadata, responsePayload);
        when(requester.request(any(), any())).thenReturn(envelope);
        final boolean isUserHasPermissionForApplicationTypeCode = userDetailsLoader.isUserHasPermissionForApplicationTypeCode(metadata, requester, applicationTypeCode);
        assertThat(isUserHasPermissionForApplicationTypeCode, is(false));
    }

    @Test
    public void shouldReturnTrueForIsUserHasPermissionForApplicationTypeCodeWhenUsersGroupsQueryReturnEmptyResponse() {

        final Metadata metadata = CommandClientTestBase.metadataFor("usersgroups.is-logged-in-user-has-permission-for-object", randomUUID().toString());
        final String applicationTypeCode = "anyCode";

        final Envelope envelope = Envelope.envelopeFrom(metadata, createObjectBuilder().build());
        when(requester.request(any(), any())).thenReturn(envelope);
        final boolean isUserHasPermissionForApplicationTypeCode = userDetailsLoader.isUserHasPermissionForApplicationTypeCode(metadata, requester, applicationTypeCode);
        assertThat(isUserHasPermissionForApplicationTypeCode, is(true));
    }

    @Test
    public void shouldReturnTrueForIsUserHasPermissionForApplicationTypeCodeWhenUsersGroupsQueryReturnTrue() {

        final JsonObject responsePayload = FileUtil.jsonFromString(FileUtil
                .getPayload("json/permission-for-application-type.json")
                .replaceAll("%HAS_PERMISSION%", "true"));

        final Metadata metadata = CommandClientTestBase.metadataFor("usersgroups.is-logged-in-user-has-permission-for-object", randomUUID().toString());
        final String applicationTypeCode = "anyCode";

        final Envelope envelope = Envelope.envelopeFrom(metadata, responsePayload);
        when(requester.request(any(), any())).thenReturn(envelope);
        final boolean isUserHasPermissionForApplicationTypeCode = userDetailsLoader.isUserHasPermissionForApplicationTypeCode(metadata, requester, applicationTypeCode);
        assertThat(isUserHasPermissionForApplicationTypeCode, is(true));
    }

    @Test
    public void shouldReturnTrueForGetAllowedParentApplicationsWhenActivePermissionMatches() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, parentApplicationPermissions(true, randomUUID().toString()));
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(true));
    }

    @Test
    public void shouldRequestApplicationObjectWithCreateChildApplicationActionForGetAllowedParentApplications() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, parentApplicationPermissions(true, randomUUID().toString()));
        when(requester.requestAsAdmin(requestEnvelopeCaptor.capture(), any())).thenReturn(envelope);

        UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        final JsonObject sentRequest = requestEnvelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(sentRequest.getString("object"), is("Application"));
        assertThat(sentRequest.getString("action"), is("CreateChildApplication"));
        assertThat(sentRequest.getString("source"), is(parentApplicationTypeId));
    }

    @Test
    public void shouldReturnTrueForGetAllowedParentApplicationsWhenAtLeastOneOfMultiplePermissionsIsActive() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, parentApplicationPermissions(false, randomUUID().toString(), true, randomUUID().toString()));
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(true));
    }

    @Test
    public void shouldReturnTrueForGetAllowedParentApplicationsWhenActiveFlagIsStringTrue() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final JsonObject permissionsPayload = createObjectBuilder()
                .add("permissions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("object", "ParentApplication")
                                .add("action", "Create")
                                .add("active", "true")
                                .add("source", parentApplicationTypeId)
                                .add("target", randomUUID().toString())))
                .build();
        final Envelope envelope = Envelope.envelopeFrom(metadata, permissionsPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(true));
    }

    @Test
    public void shouldReturnFalseForGetAllowedParentApplicationsWhenNoPermissionIsActive() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, parentApplicationPermissions(false, randomUUID().toString()));
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(false));
    }

    @Test
    public void shouldReturnFalseForGetAllowedParentApplicationsWhenActiveFlagMissing() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final JsonObject permissionsPayload = createObjectBuilder()
                .add("permissions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("object", "ParentApplication")
                                .add("action", "Create")
                                .add("source", parentApplicationTypeId)
                                .add("target", randomUUID().toString())))
                .build();
        final Envelope envelope = Envelope.envelopeFrom(metadata, permissionsPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(false));
    }

    @Test
    public void shouldReturnFalseForGetAllowedParentApplicationsWhenPermissionsKeyMissing() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, createObjectBuilder().build());
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(false));
    }

    @Test
    public void shouldReturnFalseForGetAllowedParentApplicationsWhenPermissionsArrayIsEmpty() {
        final String parentApplicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final JsonObject permissionsPayload = createObjectBuilder().add("permissions", createArrayBuilder()).build();
        final Envelope envelope = Envelope.envelopeFrom(metadata, permissionsPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final boolean allowed = UserDetailsLoader.getAllowedParentApplications(metadata, requester, parentApplicationTypeId);

        assertThat(allowed, is(false));
    }

    @Test
    public void shouldReturnAllowedHearingTypesOnlyForActivePermissions() {
        final String applicationTypeId = randomUUID().toString();
        final UUID activeHearingType1 = randomUUID();
        final UUID activeHearingType2 = randomUUID();
        final UUID inactiveHearingType = randomUUID();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());

        final JsonArrayBuilder permissions = createArrayBuilder()
                .add(hearingTypePermission(applicationTypeId, activeHearingType1.toString(), true))
                .add(hearingTypePermission(applicationTypeId, inactiveHearingType.toString(), false))
                .add(hearingTypePermission(applicationTypeId, activeHearingType2.toString(), true));
        final JsonObject permissionsPayload = createObjectBuilder().add("permissions", permissions).build();
        final Envelope envelope = Envelope.envelopeFrom(metadata, permissionsPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final List<UUID> allowedHearingTypes = UserDetailsLoader.getAllowedHearingTypes(metadata, requester, applicationTypeId);

        assertThat(allowedHearingTypes, containsInAnyOrder(activeHearingType1, activeHearingType2));
    }

    @Test
    public void shouldReturnEmptyListForGetAllowedHearingTypesWhenPermissionsKeyMissing() {
        final String applicationTypeId = randomUUID().toString();
        final Metadata metadata = CommandClientTestBase.metadataFor(USER_GROUPS_GET_PERMISSION, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, createObjectBuilder().build());
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);

        final List<UUID> allowedHearingTypes = UserDetailsLoader.getAllowedHearingTypes(metadata, requester, applicationTypeId);

        assertThat(allowedHearingTypes, empty());
    }

    private JsonObject hearingTypePermission(final String source, final String target, final boolean active) {
        return createObjectBuilder()
                .add("object", "HearingType")
                .add("action", "Locked")
                .add("active", active)
                .add("source", source)
                .add("target", target)
                .build();
    }

    private JsonObject parentApplicationPermissions(final boolean firstActive, final String firstTarget, final Object... rest) {
        final JsonArrayBuilder permissions = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("object", "ParentApplication")
                        .add("action", "Create")
                        .add("active", firstActive)
                        .add("source", firstTarget)
                        .add("target", randomUUID().toString()));
        for (int i = 0; i < rest.length; i += 2) {
            final boolean active = (boolean) rest[i];
            final String target = (String) rest[i + 1];
            permissions.add(createObjectBuilder()
                    .add("object", "ParentApplication")
                    .add("action", "Create")
                    .add("active", active)
                    .add("source", target)
                    .add("target", randomUUID().toString()));
        }
        return createObjectBuilder().add("permissions", permissions).build();
    }

}