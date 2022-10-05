package uk.gov.moj.cpp.progression.query.api.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.query.FormQueryView;
import uk.gov.moj.cpp.progression.query.HearingQueryView;
import uk.gov.moj.cpp.progression.query.PetQueryView;

import javax.json.JsonObject;

public class ProgressionService {

    private static final String CASE_ID = "caseId";
    private static final String PET_ID = "petId";
    private static final String STRUCTURED_FORM_ID = "structuredFormId";
    private static final String COURT_FORM_ID = "courtFormId";
    private static final String FORM_ID = "formId";
    private static final String DATA = "data";
    private static final String LAST_UPDATED = "lastUpdated";

    public JsonObject getPetsForCase(final PetQueryView petQueryView, final JsonEnvelope query, final String caseId) {
        final JsonEnvelope petsForCase = petQueryView.getPetsForCase(envelopeFrom(metadataFrom(query.metadata()).withName("progression.query.pets-for-case"), createObjectBuilder()
                .add(CASE_ID, caseId)
                .build()));

        return petsForCase.payloadAsJsonObject();
    }

    public JsonObject getPet(final Requester requester, final JsonEnvelope envelope, final String petId) {
        final JsonEnvelope materialResponse = requester.request(envelopeFrom(metadataFrom(envelope.metadata()).withName("material.query.structured-form"), createObjectBuilder()
                .add(STRUCTURED_FORM_ID, petId)
                .build()));
        return createObjectBuilder()
                .add(PET_ID, materialResponse.payloadAsJsonObject().getString(STRUCTURED_FORM_ID))
                .add(FORM_ID, materialResponse.payloadAsJsonObject().getString(FORM_ID))
                .add(DATA, materialResponse.payloadAsJsonObject().getString(DATA))
                .add(LAST_UPDATED, materialResponse.payloadAsJsonObject().getString(LAST_UPDATED))
                .build();
    }

    public JsonObject getHearing(final HearingQueryView hearingQueryView, final JsonEnvelope envelope, final String hearingId) {

        final MetadataBuilder metadataBuilder = metadataFrom(envelope.metadata()).withName("progression.query.hearing");
        final JsonObject payloadJson= createObjectBuilder()
                .add("hearingId", hearingId)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder, payloadJson);
        final JsonEnvelope response = hearingQueryView.getHearing(requestEnvelope);
        return response.payloadAsJsonObject();

    }

    public JsonObject getFormsForCase(final FormQueryView formQueryView, final JsonEnvelope query, final String caseId) {
        final JsonEnvelope petsForCase = formQueryView.getFormsForCase(envelopeFrom(metadataFrom(query.metadata()).withName("progression.query.forms-for-case"), createObjectBuilder()
                .add(CASE_ID, caseId)
                .build()));

        return petsForCase.payloadAsJsonObject();
    }

    public JsonObject getForm(final FormQueryView formQueryView, final JsonEnvelope query, final String caseId, final String formId) {
        final JsonEnvelope petsForCase = formQueryView.getForm(envelopeFrom(metadataFrom(query.metadata()).withName("progression.query.form"), createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(COURT_FORM_ID, formId)
                .build()));

        return petsForCase.payloadAsJsonObject();
    }

}
