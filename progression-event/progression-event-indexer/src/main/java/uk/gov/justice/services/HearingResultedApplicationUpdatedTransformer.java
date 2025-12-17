package uk.gov.justice.services;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.services.transformer.BaseCourtApplicationTransformer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

@SuppressWarnings("squid:S2629")
public class HearingResultedApplicationUpdatedTransformer extends BaseCourtApplicationTransformer {

    @Override
    public Object transform(final Object input) {
        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final HearingResultedApplicationUpdated courtApplicationCreated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, HearingResultedApplicationUpdated.class);

        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();
        return  createCaseDocumentsFromCourtApplication(courtApplication);
    }

    @Override
    protected String getDefaultCaseStatus() {
        return ACTIVE;
    }
}
