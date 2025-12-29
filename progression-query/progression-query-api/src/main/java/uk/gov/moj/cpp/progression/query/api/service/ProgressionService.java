package uk.gov.moj.cpp.progression.query.api.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.query.FormQueryView;
import uk.gov.moj.cpp.progression.query.HearingQueryView;
import uk.gov.moj.cpp.progression.query.PetQueryView;
import uk.gov.moj.cpp.progression.query.view.service.exception.ProgressionServiceException;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import java.util.UUID;

public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);

    private static final String PROSECUTION_CASE = "progression.query.case";

    private static final String QUERY_PET = "progression.query.pet";

    private static final String QUERY_PTPH = "progression.query.form";

    public static final String PROGRESSION_GET_CASE_HEARING_TYPE = "progression.query.case.hearingtypes";

    private static final String CASE_ID = "caseId";

    private static final String PET_ID = "petId";
    private static final String STRUCTURED_FORM_ID = "structuredFormId";
    private static final String COURT_FORM_ID = "courtFormId";
    private static final String FORM_ID = "formId";
    private static final String DATA = "data";
    private static final String LAST_UPDATED = "lastUpdated";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;



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

    public ProsecutionCase getProsecutionCase(final UUID caseId, final Requester requester, final JsonEnvelope query) {

        LOGGER.info(" Calling {} to get prosecution case for {} ", PROSECUTION_CASE, caseId);

        final JsonObject requestJson = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        final JsonEnvelope responseEnvelope = requester.request(envelop(requestJson)
                .withName(PROSECUTION_CASE).withMetadataFrom(query));

        if (isNull(responseEnvelope.payloadAsJsonObject()) || isNull(responseEnvelope.payloadAsJsonObject().getJsonObject("prosecutionCase"))) {
            throw new ProgressionServiceException(format("Failed to get prosecution case from progression for caseId %s", caseId));
        }

        LOGGER.info("Got prosecution - {}", responseEnvelope.payloadAsJsonObject());
        return jsonObjectToObjectConverter.convert(responseEnvelope.payloadAsJsonObject().getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }

    public JsonObject getPetForm(final UUID petFormId, final Requester requester, final JsonEnvelope query) {

        LOGGER.info(" Calling {} to get pet form {} ", QUERY_PET, petFormId);

        final JsonObject requestJson = createObjectBuilder()
                .add(PET_ID, petFormId.toString())
                .build();
        try {
            final JsonEnvelope responseEnvelope = requester.request(envelop(requestJson)
                    .withName(QUERY_PET).withMetadataFrom(query));
            return responseEnvelope.payloadAsJsonObject();
        }catch(Exception e){
            LOGGER.error("PET form Not Found or Error while fetching {}",e);
            return JsonObjects.createObjectBuilder().build();
        }
    }

    public JsonObject getPtphForm(final UUID caseId, final UUID courtFormId, final Requester requester, final JsonEnvelope query) {

        LOGGER.info(" Calling {} to get court form {} ", QUERY_PTPH, courtFormId);

        final JsonObject requestJson = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(COURT_FORM_ID, courtFormId.toString())
                .build();
        try {
            final JsonEnvelope responseEnvelope = requester.request(envelop(requestJson)
                    .withName(QUERY_PTPH).withMetadataFrom(query));
            LOGGER.info(" Progression.Query.Form call -> {} ", responseEnvelope.payloadAsJsonObject());
            return responseEnvelope.payloadAsJsonObject();
        }catch(Exception e){
            LOGGER.error("PTPH form Not Found or Error while fetching {}",e);
            return JsonObjects.createObjectBuilder().build();
        }
    }
}
