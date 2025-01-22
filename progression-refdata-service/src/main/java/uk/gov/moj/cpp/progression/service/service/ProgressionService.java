package uk.gov.moj.cpp.progression.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.exception.ProgressionServiceException;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@SuppressWarnings({"squid:S2221"})
public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);

    private static final String PROSECUTION_CASE = "progression.query.case";

    private static final String QUERY_PET = "progression.query.pet";

    private static final String QUERY_PTPH = "progression.query.form";

    public static final String PROGRESSION_GET_CASE_HEARING_TYPE = "progression.query.case.hearingtypes";

    private static final String CASE_ID = "caseId";

    private static final String PET_ID = "petId";

    private static final String COURT_FORM_ID = "courtFormId";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

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
            return Json.createObjectBuilder().build();
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
            return Json.createObjectBuilder().build();
        }
    }
}
