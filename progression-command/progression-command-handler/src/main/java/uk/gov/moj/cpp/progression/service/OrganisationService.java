package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

public class OrganisationService {

    private static final String DEFENCE_ASSOCIATION_QUERY = "progression.query.associated-organisation";
    private static final String ASSOCIATION = "association";

    private static final String DEFENCE_ASSOCIATION_ORGANISATION_QUERY = "defence.query.associated-organisation";

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;


    public JsonObject getAssociatedOrganisation(final Envelope<?> envelope, final String defendantId) {

        final JsonObject getUserGroupsForUserRequest = Json.createObjectBuilder().add("defendantId", defendantId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getUserGroupsForUserRequest)
                .withName(DEFENCE_ASSOCIATION_QUERY).withMetadataFrom(envelope);
        final JsonEnvelope response = requester.request(requestEnvelope);
        return response.payloadAsJsonObject().getJsonObject(ASSOCIATION);
    }

    public JsonObject getAssociatedOrganisationForApplication(final Envelope<?> envelope, final String defendantId) {

        final JsonObject getUserGroupsForUserRequest = Json.createObjectBuilder().add("defendantId", defendantId).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getUserGroupsForUserRequest)
                .withName(DEFENCE_ASSOCIATION_ORGANISATION_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload().getJsonObject(ASSOCIATION);
    }

}
