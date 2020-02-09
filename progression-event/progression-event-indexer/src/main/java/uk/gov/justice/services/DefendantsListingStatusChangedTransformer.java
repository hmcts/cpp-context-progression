package uk.gov.justice.services;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefendantsListingStatusChangedTransformer implements Transform {

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ApplicationMapper applicationMapper = new ApplicationMapper();
    private CaseDetailsMapper caseDetailsMapper = new CaseDetailsMapper();
    private HearingMapper hearingMapper = new HearingMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final ProsecutionCaseDefendantListingStatusChanged listingStatusChanged =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, ProsecutionCaseDefendantListingStatusChanged.class);

        final Hearing hearing = listingStatusChanged.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();
        prosecutionCases(listingStatusChanged, hearing, prosecutionCases, caseDocumentsMap);
        courtApplications(listingStatusChanged, hearing, caseDocumentsMap);
        final List<CaseDetails> caseDetailsList = caseDocumentsMap.values().stream().collect(toList());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    @SuppressWarnings("squid:S3824")
    private void prosecutionCases(final ProsecutionCaseDefendantListingStatusChanged listingStatusChanged,
                                  final Hearing hearing,
                                  final List<ProsecutionCase> prosecutionCases,
                                  final Map<UUID, CaseDetails> caseDocumentsMap) {
        if (prosecutionCases!= null) {
            for (final ProsecutionCase prosecutionCase : prosecutionCases) {
                final List<String> defendantIds = new ArrayList<>();
                final List<Defendant> defendants = prosecutionCase.getDefendants();
                for (final Defendant defendant : defendants) {
                    defendantIds.add(defendant.getId().toString());
                    final UUID prosecutionCaseId = prosecutionCase.getId();
                    CaseDetails caseDetailsExisting = caseDocumentsMap.get(prosecutionCaseId);
                    caseDetailsExisting = caseDetails("PROSECUTION", hearing, prosecutionCaseId, caseDetailsExisting);
                    caseDetailsExisting.setHearings(hearings(listingStatusChanged, defendantIds));
                    caseDocumentsMap.put(prosecutionCaseId, caseDetailsExisting);
                }
            }
        }
    }

    @SuppressWarnings("squid:S3824")
    private void courtApplications(final ProsecutionCaseDefendantListingStatusChanged listingStatusChanged,
                                   final Hearing hearing,
                                   final Map<UUID, CaseDetails> caseDocumentsMap) {
        final List<CourtApplication> courtApplications = hearing.getCourtApplications();
        if (courtApplications!= null) {
            for (final CourtApplication courtApplication : courtApplications) {
                final UUID caseId = courtApplication.getLinkedCaseId();
                final UUID applicationId = courtApplication.getId();

                UUID caseDetailsId = caseId;
                if (caseId == null) {
                    caseDetailsId = applicationId;
                }
                CaseDetails caseDetailsExisting = caseDocumentsMap.get(caseDetailsId);
                List<Application> applications = new ArrayList<>();
                caseDetailsExisting = caseDetails("APPLICATION", hearing, caseDetailsId, caseDetailsExisting);
                applications.add(applicationMapper.transform(courtApplication));
                caseDetailsExisting.setHearings(hearings(listingStatusChanged, null));
                caseDetailsExisting.setApplications(applications);
                caseDocumentsMap.put(caseDetailsId, caseDetailsExisting);
            }
        }
    }


    private List<uk.gov.justice.services.unifiedsearch.client.domain.Hearing> hearings(final ProsecutionCaseDefendantListingStatusChanged listingStatusChanged,
                                                                                       final List<String> defendantIds) {
        final List<uk.gov.justice.services.unifiedsearch.client.domain.Hearing> hearings = new ArrayList<>();
        hearings.add(hearingMapper.hearing(listingStatusChanged, defendantIds));
        return hearings;
    }

    private CaseDetails caseDetails(final String caseType, Hearing hearing, UUID caseDetailsId, CaseDetails caseDetailsExisting) {
        if (caseDetailsExisting == null) {
            caseDetailsExisting = caseDetailsMapper.transform(caseType, caseDetailsId, hearing.getJurisdictionType());
        }
        return caseDetailsExisting;
    }

}