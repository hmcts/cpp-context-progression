package uk.gov.moj.cpp.progression.query.view.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class OrganisationService {

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    static final String DEFENCE_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_QUERY = "defence.query.case-defendants-organisation";
    private static final String CASE_DEFENDANT_ORGANISATION = "caseDefendantOrganisation";

    public JsonObject getAssociatedCaseDefendantsWithOrganisationAddress(final Envelope<?> envelope, final String caseId) {

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
