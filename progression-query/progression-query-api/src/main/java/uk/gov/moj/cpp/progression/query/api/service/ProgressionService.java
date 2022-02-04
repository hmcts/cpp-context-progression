package uk.gov.moj.cpp.progression.query.api.service;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

public class ProgressionService {
    public static final String CASE_ID = "caseId";
    public JsonObject getPetsForCase(final Requester requester, final JsonEnvelope query, final String caseId){
        final JsonEnvelope petsForCase = requester.request(envelopeFrom(metadataFrom(query.metadata()).withName("progression.query.pets-for-case"), createObjectBuilder()
                .add(CASE_ID, caseId)
                .build()));

        return petsForCase.payloadAsJsonObject();
    }

}
