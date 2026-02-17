package uk.gov.justice.services;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.services.transformer.BaseCourtApplicationTransformer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

public class HearingInitiateEnrichedTransformer extends BaseCourtApplicationTransformer {

    private HearingMapper hearingMapper = new HearingMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final HearingInitiateEnriched hearingInitiateEnriched = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, HearingInitiateEnriched.class);

        final Hearing hearing = hearingInitiateEnriched.getHearing();

        if (hearingIsNotBreachTypeApplication(hearing)) {
            return emptyPayload();
        }

        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();

        courtApplications(hearing, caseDocumentsMap);
        final List<CaseDetails> caseDetailsList = caseDocumentsMap.values().stream().collect(toList());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    private boolean hearingIsNotBreachTypeApplication(final Hearing hearing) {
        return isEmpty(hearing.getCourtApplications()) || isNoneApplicationHasCourtOrder(hearing.getCourtApplications());
    }

    private boolean isNoneApplicationHasCourtOrder(final List<CourtApplication> courtApplications) {
        return courtApplications.stream()
                .map(CourtApplication::getCourtOrder)
                .allMatch(java.util.Objects::isNull);
    }

    private Object emptyPayload() {
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", emptyList());
        return caseDocuments;
    }

    @Override
    public String getDefaultCaseStatus() {
        //caseStatus should not be set as we do not want to overwrite existing info
        return null;
    }

    private void courtApplications(final Hearing hearing, final Map<UUID, CaseDetails> caseDocumentsMap) {
        final List<CourtApplication> courtApplications = hearing.getCourtApplications();

        if (isNotEmpty(courtApplications)) {

            for (final CourtApplication courtApplication : courtApplications) {
                transformCourtApplication(courtApplication, hearing.getJurisdictionType(), hearings(hearing, null), caseDocumentsMap);
            }

        }
    }

    private List<uk.gov.justice.services.unifiedsearch.client.domain.Hearing> hearings(final Hearing hearing, final List<String> defendantIds) {
        final List<uk.gov.justice.services.unifiedsearch.client.domain.Hearing> hearings = new ArrayList<>();
        hearings.add(hearingMapper.hearing(hearing, defendantIds));
        return hearings;
    }

}
