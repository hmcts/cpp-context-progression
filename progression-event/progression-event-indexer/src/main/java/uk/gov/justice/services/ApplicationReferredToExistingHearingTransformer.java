package uk.gov.justice.services;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.services.transformer.BaseCourtApplicationTransformer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

@SuppressWarnings("squid:S2629")
public class ApplicationReferredToExistingHearingTransformer extends BaseCourtApplicationTransformer {

    @Override
    public Object transform(final Object input) {
        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final ApplicationReferredToExistingHearing applicationReferredToExistingHearing =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, ApplicationReferredToExistingHearing.class);

        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();
        final CourtApplication courtApplication = applicationReferredToExistingHearing.getApplication();
        transformCourtApplicationStatusChange(courtApplication, caseDocumentsMap);

        final List<CaseDetails> caseDetailsList = caseDocumentsMap.values().stream().collect(Collectors.toList());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    @Override
    protected String getDefaultCaseStatus() {
        return ACTIVE;
    }
}
