package uk.gov.moj.cpp.progression.query.api.service;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import javax.json.JsonObject;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

public class OrganisationService {

    static final String DEFENCE_ASSOCIATION_QUERY = "defence.query.associated-organisation";
    static final String DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY = "defence.query.case-defendants-organisation";
    private static final String ASSOCIATION = "association";
    private static final String CASE_DEFENDANT_ORGANISATION = "caseDefendantOrganisation";


    public JsonObject getAssociatedOrganisation(final Envelope<?> envelope, final String defendantId, final Requester requester) {

        final JsonObject getUserGroupsForUserRequest = createObjectBuilder().add("defendantId", defendantId).build();
        final Envelope<JsonObject> requestEnvelope = envelop(getUserGroupsForUserRequest)
                .withName(DEFENCE_ASSOCIATION_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload().getJsonObject(ASSOCIATION);
    }

    public JsonObject getAssociatedCaseDefendantsWithOrganisationAddress(final Envelope<?> envelope, final String caseId, final Requester requester) {

        final JsonObject getUserGroupsForUserRequest = createObjectBuilder()
                .add("caseId", caseId)
                .add("withAddress", true)
                .build();
        final Envelope<JsonObject> requestEnvelope = envelop(getUserGroupsForUserRequest)
                .withName(DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload().getJsonObject(CASE_DEFENDANT_ORGANISATION);
    }
}
