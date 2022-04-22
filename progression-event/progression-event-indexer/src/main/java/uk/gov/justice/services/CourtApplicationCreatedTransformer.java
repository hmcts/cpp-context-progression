package uk.gov.justice.services;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.services.transformer.BaseCourtApplicationTransformer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

@SuppressWarnings("squid:S2629")
public class CourtApplicationCreatedTransformer extends BaseCourtApplicationTransformer {

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final CourtApplicationCreated courtApplicationCreated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CourtApplicationCreated.class);

        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();
        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();
        transformCourtApplication(courtApplication, caseDocumentsMap);
        final List<CaseDetails> caseDetailsList = caseDocumentsMap.values().stream().collect(Collectors.toList());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    @Override
    public String getDefaultCaseStatus(){
        return ACTIVE;
    }

}
