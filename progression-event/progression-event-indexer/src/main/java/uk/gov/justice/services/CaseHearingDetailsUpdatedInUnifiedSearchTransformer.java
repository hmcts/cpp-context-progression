package uk.gov.justice.services;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.CaseHearingDetailsUpdatedInUnifiedSearch;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.services.transformer.BaseCourtApplicationTransformer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

public class CaseHearingDetailsUpdatedInUnifiedSearchTransformer extends BaseCourtApplicationTransformer {

    private HearingMapper hearingMapper = new HearingMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final CaseHearingDetailsUpdatedInUnifiedSearch laaCaseHearingDetailsUpdatedInUnifiedSearch =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CaseHearingDetailsUpdatedInUnifiedSearch.class);

        final Hearing hearing = laaCaseHearingDetailsUpdatedInUnifiedSearch.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();
        prosecutionCases(hearing, prosecutionCases, caseDocumentsMap);
        courtApplications(hearing, caseDocumentsMap);
        final List<CaseDetails> caseDetailsList = caseDocumentsMap.values().stream().collect(toList());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    @Override
    public String getDefaultCaseStatus() {
        //caseStatus should not be set as we do not want to overwrite existing info
        return null;
    }

    @SuppressWarnings({"squid:S3824"})
    private void prosecutionCases(final Hearing hearing,
                                  final List<ProsecutionCase> prosecutionCases,
                                  final Map<UUID, CaseDetails> caseDocumentsMap) {
        if (prosecutionCases != null) {
            for (final ProsecutionCase prosecutionCase : prosecutionCases) {
                final List<String> defendantIds = new ArrayList<>();
                final List<Defendant> defendants = prosecutionCase.getDefendants();

                for (final Defendant defendant : defendants) {
                    defendantIds.add(defendant.getId().toString());
                    final UUID prosecutionCaseId = prosecutionCase.getId();
                    CaseDetails caseDetailsExisting = caseDocumentsMap.get(prosecutionCaseId);
                    caseDetailsExisting = caseDetails(PROSECUTION, hearing, prosecutionCaseId, caseDetailsExisting);
                    caseDetailsExisting.setHearings(hearings(hearing, defendantIds));
                    populateProsecutingAuthorityDetails(prosecutionCase, caseDetailsExisting);
                    caseDetailsExisting.setParties(null);
                    caseDocumentsMap.put(prosecutionCaseId, caseDetailsExisting);
                }
            }
        }
    }

    private void populateProsecutingAuthorityDetails(final ProsecutionCase prosecutionCase, final CaseDetails caseDetails) {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Prosecutor prosecutor = prosecutionCase.getProsecutor();
        if (prosecutionCaseIdentifier != null) {
            if (prosecutionCaseIdentifier.getCaseURN() != null) {
                caseDetails.setCaseReference(prosecutionCaseIdentifier.getCaseURN());
            }
            if (prosecutionCaseIdentifier.getProsecutionAuthorityReference() != null) {
                caseDetails.setCaseReference(prosecutionCaseIdentifier.getProsecutionAuthorityReference());
            }
            if (nonNull(prosecutor)) {
                caseDetails.setProsecutingAuthority(prosecutor.getProsecutorCode());
            } else {
                caseDetails.setProsecutingAuthority(prosecutionCaseIdentifier.getProsecutionAuthorityCode());
            }

        }
    }


    private void courtApplications(final Hearing hearing,
                                   final Map<UUID, CaseDetails> caseDocumentsMap) {
        final List<CourtApplication> courtApplications = hearing.getCourtApplications();

        if (CollectionUtils.isNotEmpty(courtApplications)) {

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

    private CaseDetails caseDetails(final String caseType, Hearing hearing, UUID caseDetailsId, CaseDetails caseDetailsExisting) {
        if (caseDetailsExisting == null) {
            caseDetailsExisting = caseDetailsMapper.transform(caseType, caseDetailsId, hearing.getJurisdictionType());
        }
        return caseDetailsExisting;
    }
}
