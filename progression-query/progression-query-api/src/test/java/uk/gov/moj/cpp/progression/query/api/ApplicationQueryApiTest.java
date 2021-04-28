package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationQueryApiTest {

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

    @InjectMocks
    private ApplicationQueryApi applicationQueryApi;

    @Captor
    private ArgumentCaptor<String> responsePutKeys;

    @Captor
    private ArgumentCaptor<JsonObject> responsePutValues;
    private Object suppliedObject;

    @Before
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
        when(query.payloadAsJsonObject()).thenReturn(queryJson);

        when(responseJson.containsKey("assignedUser")).thenReturn(true);
        when(responseJson.getJsonObject("assignedUser")).thenReturn(assignedUserJson);
        final HashSet keys = new HashSet();
        keys.add("assignedUser");
        when(responseJson.keySet()).thenReturn(keys);

        when(assignedUserJson.getString("userId")).thenReturn(userDetails.getUserId().toString());


        when(requester.request(query)).thenReturn(response);
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
        when(requester.request(query)).thenReturn(response);
        assertThat(applicationQueryApi.getCourtApplicationForApplicationAtAGlance(query), equalTo(response));
    }

    @Test
    public void shouldGetCourtApplicationOnly() {
        when(requester.request(query)).thenReturn(response);
        assertThat(applicationQueryApi.getApplicationOnly(query), equalTo(response));
    }

    @Test
    public void shouldGetCourtProceedingsForApplication() {
        when(requester.request(query)).thenReturn(response);
        assertThat(applicationQueryApi.getCourtProceedingsForApplication(query), equalTo(response));
    }

    @Test
    public void shouldGetApplicationHearings() {
        when(requester.request(query)).thenReturn(response);
        assertThat(applicationQueryApi.getApplicationHearings(query), equalTo(response));
    }
}
