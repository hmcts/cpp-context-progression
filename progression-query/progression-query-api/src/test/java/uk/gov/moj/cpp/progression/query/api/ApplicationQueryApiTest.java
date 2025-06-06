package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.ApplicationHearingQueryView;
import uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView;
import uk.gov.moj.cpp.progression.query.ApplicationQueryView;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationQueryApiTest {

    public static final String APPLICATION_AT_GLANCE_DEFENCE = "progression.query.application.aaag-for-defence";

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private JsonObject responseJson;

    @Mock
    private JsonObject queryJson;

    @Mock
    private JsonObject assignedUserJson;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Requester requester;

    @Mock
    private ApplicationQueryView applicationQueryView;

    @Mock
    private ApplicationHearingQueryView applicationHearingQueryView;

    @InjectMocks
    private ApplicationQueryApi applicationQueryApi;

    @Mock
    private ApplicationNotesQueryView applicationNotesQueryView;

    @Mock
    private DefenceQueryService defenceQueryService;

    @Mock
    private UsersGroupQueryService usersGroupQueryService;

    private Object suppliedObject;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleApplicationQuery() {
        final UserGroupsUserDetails userDetails = new UserGroupsUserDetails();
        userDetails.setFirstName("andrew");
        userDetails.setLastName("eldritch");
        userDetails.setUserId(UUID.randomUUID());

        when(userDetailsLoader.getUserById(requester, query, userDetails.getUserId())).thenReturn(userDetails);

        when(responseJson.containsKey("assignedUser")).thenReturn(true);
        when(responseJson.getJsonObject("assignedUser")).thenReturn(assignedUserJson);
        final HashSet keys = new HashSet();
        keys.add("assignedUser");
        when(responseJson.keySet()).thenReturn(keys);

        when(assignedUserJson.getString("userId")).thenReturn(userDetails.getUserId().toString());


        when(applicationQueryView.getApplication(query)).thenReturn(response);
        when(response.payloadAsJsonObject()).thenReturn(responseJson);


        final Function<Object, JsonEnvelope> f = (obj) -> {
            suppliedObject = obj;
            return response;
        };

        when(enveloper.withMetadataFrom(Mockito.any(JsonEnvelope.class), Mockito.any(String.class))).thenReturn(f);

        assertThat(applicationQueryApi.getApplication(query), equalTo(response));

        final JsonObject suppliedJson = (JsonObject) suppliedObject;
        assertThat("andrew", equalTo(suppliedJson.getJsonObject("assignedUser").getString("firstName")));


    }

    @Test
    public void shouldGetCourtApplicationForApplicationAtAGlance() {
        when(applicationQueryView.getCourtApplicationForApplicationAtAGlance(query)).thenReturn(response);
        assertThat(applicationQueryApi.getCourtApplicationForApplicationAtAGlance(query), equalTo(response));
    }

    @Test
    public void shouldGetCourtApplicationOnly() {
        when(applicationQueryView.getApplicationOnly(query)).thenReturn(response);
        assertThat(applicationQueryApi.getApplicationOnly(query), equalTo(response));
    }

    @Test
    public void shouldGetCourtProceedingsForApplication() {
        when(applicationQueryView.getCourtProceedingsForApplication(query)).thenReturn(response);
        assertThat(applicationQueryApi.getCourtProceedingsForApplication(query), equalTo(response));
    }

    @Test
    public void shouldGetApplicationHearings() {
        when(applicationQueryView.getApplicationHearings(query)).thenReturn(response);
        assertThat(applicationQueryApi.getApplicationHearings(query), equalTo(response));
    }

    @Test
    public void shouldThrowForbiddenRequestExceptionWhenGetApplicationAtAGlanceForDefenceAndNoLinkedCasesFound() {

        final JsonObject jsonObjectPayload = createObjectBuilder().build();
        final Metadata metadata = QueryClientTestBase.metadataFor(APPLICATION_AT_GLANCE_DEFENCE);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(applicationQueryView.getCourtApplicationForApplicationAtAGlance(any())).thenReturn(envelope);
        when(usersGroupQueryService.getUserGroups(any(), any())).thenReturn(Json.createObjectBuilder()
                .add("groups", createArrayBuilder()
                        .add(createObjectBuilder().add("groupName", "Non CPS Prosecutors").build())
                        .build())
                .build());

        assertThrows(ForbiddenRequestException.class, () -> applicationQueryApi.getCourtApplicationForApplicationAtAGlanceForDefence(envelope));
    }

    @Test
    public void shouldThrowForbiddenRequestExceptionWhenGetApplicationAtAGlanceForDefenceAndUserNotInAdvocateRoleForTheCase() {

        String caseId = randomUUID().toString();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("prosecutionCaseId", caseId);
        final JsonObject jsonObjectPayload = createObjectBuilder().add("linkedCases", jsonArrayBuilder.add(jsonObjectBuilder).build()).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(APPLICATION_AT_GLANCE_DEFENCE);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(applicationQueryView.getCourtApplicationForApplicationAtAGlance(any())).thenReturn(envelope);
        when(defenceQueryService.isUserProsecutingOrDefendingCase(envelope, caseId)).thenReturn(false);
        when(usersGroupQueryService.getUserGroups(any(), any())).thenReturn(Json.createObjectBuilder()
                .add("groups", createArrayBuilder()
                        .build())
                .build());

        assertThrows(ForbiddenRequestException.class, () -> applicationQueryApi.getCourtApplicationForApplicationAtAGlanceForDefence(envelope));
    }

    @Test
    public void shouldReturnApplicationDetailsWhenGetApplicationAtAGlanceForDefenceAndUserInAdvocateRoleForTheCase() {

        String caseId = randomUUID().toString();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("prosecutionCaseId", caseId);
        final JsonObject jsonObjectPayload = createObjectBuilder().add("linkedCases", jsonArrayBuilder.add(jsonObjectBuilder).build()).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(APPLICATION_AT_GLANCE_DEFENCE);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(applicationQueryView.getCourtApplicationForApplicationAtAGlance(any())).thenReturn(envelope);
        when(usersGroupQueryService.getUserGroups(any(), any())).thenReturn(Json.createObjectBuilder()
                .add("groups", createArrayBuilder()
                        .add(createObjectBuilder().add("groupName", "Non CPS Prosecutors").build())
                        .build())
                .build());

        JsonEnvelope response = applicationQueryApi.getCourtApplicationForApplicationAtAGlanceForDefence(envelope);

        assertThat(response, equalTo(envelope));
    }

    @Test
    public void shouldGetApplicationNotesByApplicationId() {
        when(applicationNotesQueryView.getApplicationNotes(query)).thenReturn(response);
        assertThat(applicationQueryApi.getApplicationNotes(query), equalTo(response));
    }

    @Test
    public void shouldGetCaseStatusForApplicationId() {
        when(applicationQueryView.getCaseStatusForApplication(query)).thenReturn(response);
        assertThat(applicationQueryApi.getCaseStatusForApplication(query), equalTo(response));
    }

    @Test
    void shouldGetApplicationHearingCaseDetails() {
        when(applicationHearingQueryView.getApplicationHearingCaseDetails(query)).thenReturn(response);
        assertThat(applicationQueryApi.getApplicationHearingCaseDetails(query), equalTo(response));
    }

}
