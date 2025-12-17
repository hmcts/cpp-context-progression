package uk.gov.justice.services;

import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProsecutorUpdatedTransformer implements Transform {

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CaseCpsProsecutorUpdated.class);
        final UUID prosecutionCaseId = caseCpsProsecutorUpdated.getProsecutionCaseId();
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(prosecutionCaseId.toString());
        caseDetails.set_case_type("PROSECUTION");
        caseDetails.setProsecutingAuthority(caseCpsProsecutorUpdated.getProsecutionAuthorityCode());
        return caseDetails;
    }
}