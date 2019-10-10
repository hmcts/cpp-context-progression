package uk.gov.moj.cpp.progression.query.api;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.core.courts.AssignedUser;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_API)
@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class ApplicationQueryApi {

    public static final String ASSIGNED_USER_FIELD_NAME = "assignedUser";
    @Inject
    private Requester requester;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;


    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Handles("progression.query.application")
    public JsonEnvelope getApplication(final JsonEnvelope query) {
        final JsonEnvelope appQueryResponse = requester.request(query);
        JsonEnvelope response = appQueryResponse;
        final JsonObject jsonObject = appQueryResponse.payloadAsJsonObject();
        if (jsonObject.containsKey(ASSIGNED_USER_FIELD_NAME)) {
            final UUID id = UUID.fromString(jsonObject.getJsonObject(ASSIGNED_USER_FIELD_NAME).getString("userId"));
            final UserGroupsUserDetails userGroupsUserDetails = userDetailsLoader.getUserById(requester, query, id);
            final AssignedUser assignedUser = AssignedUser.assignedUser()
                    .withLastName(userGroupsUserDetails.getLastName())
                    .withFirstName(userGroupsUserDetails.getFirstName())
                    .withUserId(id)
                    .build();
            final JsonObject userJson = objectToJsonObjectConverter.convert(assignedUser);

            final JsonObjectBuilder newPayload = createObjectBuilder();
            appQueryResponse.payloadAsJsonObject().keySet().forEach(
                    keyName -> {
                        if (ASSIGNED_USER_FIELD_NAME.equals(keyName)) {
                            newPayload.add(keyName, userJson);
                        } else {
                            newPayload.add(keyName, appQueryResponse.payloadAsJsonObject().get(keyName));
                        }
                    }
            );

            response = enveloper.withMetadataFrom(appQueryResponse, "progression.query.application")
                    .apply(newPayload.build());


        }

        return response;
    }

    @Handles("progression.query.application.summary")
    public JsonEnvelope getApplicationSummary(final JsonEnvelope query) {
        return requester.request(query);
    }

}
