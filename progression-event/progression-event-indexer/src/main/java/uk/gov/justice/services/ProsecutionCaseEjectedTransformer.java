package uk.gov.justice.services;

import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProsecutionCaseEjectedTransformer implements Transform {

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final CaseEjected caseEjected =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CaseEjected.class);
        final UUID prosecutionCaseId = caseEjected.getProsecutionCaseId();
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(prosecutionCaseId.toString());
        caseDetails.set_case_type("PROSECUTION");
        caseDetails.setCaseStatus("EJECTED");
        return caseDetails;
    }
}