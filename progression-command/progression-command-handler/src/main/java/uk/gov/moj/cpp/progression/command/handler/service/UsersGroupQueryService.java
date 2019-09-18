package uk.gov.moj.cpp.progression.command.handler.service;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsersGroupQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersGroupQueryService.class.getName());
    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;
    @Inject
    private Enveloper enveloper;

    public JsonObject getOrganisationDetailsForUser(final JsonEnvelope envelope) {

        final String userId = envelope.metadata().userId().orElseThrow(() -> new NullPointerException("User id Not Supplied for the UserGroups look up"));
        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder().add("userId", userId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        if (notFound(response)
                || (response.payloadAsJsonObject().getString("organisationId") == null)) {
            LOGGER.debug("Unable to retrieve Organisation for User {}", userId);
            throw new IllegalArgumentException(format("Missing Organisation for User %s", userId));
        }
        return response.payloadAsJsonObject();
    }

    private static boolean notFound(JsonEnvelope response) {
        final JsonValue payload = response.payload();
        return payload == null
                || payload.equals(JsonValue.NULL);
    }
}
