package uk.gov.moj.cpp.progression.command.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.command.CommandClientTestBase.readJson;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChildApplicationPermissionCheckerTest {

    private static final String USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS = "usersgroups.get-logged-in-user-permissions";

    @Mock
    private Requester requester;

    @InjectMocks
    private ChildApplicationPermissionChecker childApplicationPermissionChecker;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private final String userId = randomUUID().toString();

    private final Metadata metadata = metadataWithRandomUUID("progression.initiate-court-proceedings-for-application")
            .withUserId(userId)
            .build();

    @Test
    public void shouldHavePermissionWhenCreateChildApplicationLinkPermissionIsPresent() {
        givenPermissionsResponse(readJson("json/usersgroups.get-logged-in-user-permissions.json", JsonObject.class));

        assertThat(childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(metadata), is(true));

        verify(requester).request(envelopeCaptor.capture(), eq(JsonObject.class));
        verify(requester, never()).requestAsAdmin(any(Envelope.class), any());
        final Envelope<JsonObject> request = envelopeCaptor.getValue();
        assertThat(request.metadata().name(), is(USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS));
        assertThat(request.metadata().userId().orElse(null), is(userId));
        assertThat(request.payload().getString("userId"), is(userId));
    }

    public static Stream<JsonObject> responsesWithoutPermission() {
        return Stream.of(
                permissions(permission("Create Child Application", "View"), permission("Edit case", "Link")),
                permissions(permission("Edit Standalone application", "Link")),
                permissions(createObjectBuilder().add("permissionId", randomUUID().toString()).build()),
                createObjectBuilder().add("permissions", createArrayBuilder()).build(),
                createObjectBuilder().build());
    }

    @ParameterizedTest
    @MethodSource("responsesWithoutPermission")
    public void shouldNotHavePermissionWhenCreateChildApplicationLinkPermissionIsAbsent(final JsonObject response) {
        givenPermissionsResponse(response);

        assertThat(childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(metadata), is(false));

        verify(requester).request(envelopeCaptor.capture(), eq(JsonObject.class));
        assertThat(envelopeCaptor.getValue().metadata().name(), is(USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS));
        assertThat(envelopeCaptor.getValue().payload().getString("userId"), is(userId));
    }

    @Test
    public void shouldNotHavePermissionWhenUserIdIsAbsent() {
        final Metadata metadataWithoutUser = metadataWithRandomUUID("progression.initiate-court-proceedings-for-application").build();

        assertThat(childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(metadataWithoutUser), is(false));

        verifyNoInteractions(requester);
    }

    private void givenPermissionsResponse(final JsonObject payload) {
        when(requester.request(any(Envelope.class), eq(JsonObject.class)))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(USERSGROUPS_GET_LOGGED_IN_USER_PERMISSIONS), payload));
    }

    private static JsonObject permissions(final JsonObject... permissions) {
        return createObjectBuilder()
                .add("permissions", Stream.of(permissions).reduce(createArrayBuilder(), (array, permission) -> array.add(permission), (left, right) -> left))
                .build();
    }

    private static JsonObject permission(final String object, final String action) {
        return createObjectBuilder()
                .add("permissionId", randomUUID().toString())
                .add("object", object)
                .add("action", action)
                .add("description", "DESCRIPTION")
                .build();
    }
}
