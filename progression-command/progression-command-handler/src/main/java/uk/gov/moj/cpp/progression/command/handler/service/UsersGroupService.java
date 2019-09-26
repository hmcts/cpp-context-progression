package uk.gov.moj.cpp.progression.command.handler.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsersGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersGroupService.class.getName());
    public static final String ORGANISATION_ID = "organisationId";
    public static final String ORGANISATION_NAME = "organisationName";
    public static final String ORGANISATION_TYPE = "organisationType";
    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;
    @Inject
    private Enveloper enveloper;

    public JsonObject getOrganisationDetailsForUser(final Envelope<?> envelope) {

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new NullPointerException("User id Not Supplied for the UserGroups look up"));
        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder().add("userId", userId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getOrganisationForUserRequest)
                .withName("usersgroups.get-organisation-details-for-user").withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        if (notFound(response)
                || (response.payloadAsJsonObject().getString(ORGANISATION_ID) == null)) {
            LOGGER.debug("Unable to retrieve Organisation for User {}", userId);
            throw new IllegalArgumentException(format("Missing Organisation for User %s", userId));
        }
        return response.payloadAsJsonObject();
    }

    public OrganisationDetails getUserOrgDetails(final Envelope<?> envelope) {
        final JsonObject org = getOrganisationDetailsForUser(envelope);
        return OrganisationDetails.of(fromString(org.getString(ORGANISATION_ID)),
                org.getString(ORGANISATION_NAME),
                org.getString(ORGANISATION_TYPE));
    }

    private static boolean notFound(JsonEnvelope response) {
        final JsonValue payload = response.payload();
        return payload == null
                || payload.equals(JsonValue.NULL);
    }
}
