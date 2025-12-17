package uk.gov.justice.services;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationStatusUpdated;
import uk.gov.justice.services.transformer.BaseCourtApplicationTransformer;


import javax.json.JsonObject;

@SuppressWarnings("squid:S2629")
public class CourtApplicationStatusUpdatedTransformer extends BaseCourtApplicationTransformer {

    @Override
    public Object transform(final Object input) {
        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final CourtApplicationStatusUpdated courtApplicationStatusUpdated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CourtApplicationStatusUpdated.class);

        final CourtApplication courtApplication = courtApplicationStatusUpdated.getCourtApplication();
        return  createCaseDocumentsFromCourtApplication(courtApplication);
    }

    @Override
    protected String getDefaultCaseStatus() {
        return ACTIVE;
    }
}
