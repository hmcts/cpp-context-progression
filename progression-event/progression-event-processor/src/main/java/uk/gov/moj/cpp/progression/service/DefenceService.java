package uk.gov.moj.cpp.progression.service;


import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsOrganisations;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class DefenceService {

    private static final String CASE_DEFENDANTS_ORGANISATIONS =  "defence.query.case-defendants-organisation";
    private static final String ROLE_IN_CASE_BY_CASEID = "advocate.query.role-in-case-by-caseid";

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    public CaseDefendantsOrganisations getDefendantsAndAssociatedOrganisationsForCase(final JsonEnvelope envelope, final String caseId){

        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), CASE_DEFENDANTS_ORGANISATIONS);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("caseId", caseId)
                .build();
        return requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), CaseDefendantsOrganisations.class).payload();

    }

    public JsonObject getRoleInCaseByCaseId(final JsonEnvelope envelope, final String caseId){
        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), ROLE_IN_CASE_BY_CASEID);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("caseId", caseId)
                .build();
        return requester.request(envelopeFrom(metadata, jsonPayLoad), JsonObject.class).payload();
    }
}
