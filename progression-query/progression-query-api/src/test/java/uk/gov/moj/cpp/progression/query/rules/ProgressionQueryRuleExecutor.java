package uk.gov.moj.cpp.progression.query.rules;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.CAUSATION;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.CLIENT_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.CONTEXT;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.CORRELATION;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.SESSION_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.STREAM;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.STREAM_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.VERSION;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

@RunWith(MockitoJUnitRunner.class)
public abstract class ProgressionQueryRuleExecutor extends BaseDroolsAccessControlTest {

    private static final String USERGROUPS_QUERY = "usersgroups.get-groups-by-user";
    private static final String UUID_ID = "d04885b4-9652-4c2a-87c6-299bda0a87d4";
    private static final String UUID_CLIENT_CORRELATION = "8d67ed44-ecfb-43ce-867c-53077abf97a6";
    private static final String UUID_CAUSATION = "49ef76bc-df4f-4b91-8ca7-21972c30ee4c";
    private static final String UUID_USER_ID = "182a8f83-faa0-46d6-96d0-96999f05e3a2";
    private static final String UUID_SESSION_ID = "f0132298-7b79-4397-bab6-f2f5e27915f0";
    private static final String UUID_STREAM_ID = "f29e0415-3a3b-48d8-b301-d34faa58662a";
    private static final Long STREAM_VERSION = 99L;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Requester requester;

    @Mock
    Function<Object, JsonEnvelope> function;

    @Mock
    JsonEnvelope payload;

    @Spy
    @InjectMocks
    private UserAndGroupProvider userAndGroupProvider;

    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder()
                        .put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    protected ExecutionResults executeRules(final String actionName, String[] actionGroups,
                    String... userGroups) {
        final Set<String> groupsForTheAction = new HashSet<>(Arrays.asList(actionGroups));
        final Set<String> groupsForTheUser = new HashSet<>(Arrays.asList(userGroups));

        final Action action = createActionFor(actionName);

        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), eq(USERGROUPS_QUERY)))
                        .thenReturn(function);

        when(function.apply(any(JsonObject.class))).thenReturn(payload);

        final JsonEnvelope userGroupsResponse =
                        envelopeFrom(metadataWithDefaults(), buildGroups(userGroups));
        final JsonEnvelope emptyResponse = envelopeFrom(metadataWithDefaults(), buildGroups());

        groupsForTheAction.retainAll(groupsForTheUser);
        if (!groupsForTheAction.isEmpty()) {
            when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(userGroupsResponse);
        } else {
            when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(emptyResponse);
        }
        return executeRulesWith(action);
    }

    protected Action createActionFor(final String actionName) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ID, UUID_ID).add(NAME, actionName)
                        .add(CORRELATION,
                                        Json.createObjectBuilder().add(CLIENT_ID,
                                                        UUID_CLIENT_CORRELATION))
                        .add(CAUSATION, Json.createArrayBuilder().add(UUID_CAUSATION))
                        .add(CONTEXT, Json.createObjectBuilder().add(USER_ID, UUID_USER_ID)
                                        .add(SESSION_ID, UUID_SESSION_ID))
                        .add(STREAM, Json.createObjectBuilder().add(STREAM_ID, UUID_STREAM_ID)
                                        .add(VERSION, STREAM_VERSION))
                        .build();
        return new Action(envelopeFrom(metadataFrom(jsonObject), JsonValue.NULL));
    }

    private JsonObject buildGroups(final String... groupIds) {
        final JsonObjectBuilder json = Json.createObjectBuilder();
        final JsonArrayBuilder groupsArray = Json.createArrayBuilder();
        for (String groupId : groupIds) {
            groupsArray.add(Json.createObjectBuilder().add("groupId", groupId));
        }
        json.add("groups", groupsArray);
        return json.build();
    }

}
