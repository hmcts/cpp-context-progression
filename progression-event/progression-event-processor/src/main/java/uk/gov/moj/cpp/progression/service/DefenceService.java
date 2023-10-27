package uk.gov.moj.cpp.progression.service;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsOrganisations;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class DefenceService {

    private static final String CASE_DEFENDANTS_ORGANISATIONS =  "defence.query.case-defendants-organisation";
    private static final String ROLE_IN_CASE_BY_CASEID = "advocate.query.role-in-case-by-caseid";
    private static final String ASSOCIATED_DEFENCE_ORGANISATION = "defence.query.associated-organisation";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String ASSOCIATION = "association";

    private static final String CASE_ID =  "caseId";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    public CaseDefendantsOrganisations getDefendantsAndAssociatedOrganisationsForCase(final JsonEnvelope envelope, final String caseId){

        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), CASE_DEFENDANTS_ORGANISATIONS);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();
        return requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), CaseDefendantsOrganisations.class).payload();

    }

    public JsonObject getRoleInCaseByCaseId(final JsonEnvelope envelope, final String caseId){
        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), ROLE_IN_CASE_BY_CASEID);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        return requester.request(envelopeFrom(metadata, jsonPayLoad), JsonObject.class).payload();
    }

    public AssociatedDefenceOrganisation getDefenceOrganisationByDefendantId(final JsonEnvelope query, final UUID defendantId) {
        final JsonObject requestJson = createObjectBuilder().add(DEFENDANT_ID, defendantId.toString()).build();
        final Envelope<JsonObject> requestEnvelope = envelop(requestJson).withName(ASSOCIATED_DEFENCE_ORGANISATION).withMetadataFrom(query);

        final JsonEnvelope responseEnvelope = requester.request(requestEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject().getJsonObject(ASSOCIATION);
        if (jsonObject.isEmpty()) {
            return null;
        }
        return jsonObjectToObjectConverter.convert(jsonObject, AssociatedDefenceOrganisation.class);
    }

}
