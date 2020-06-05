package uk.gov.justice.services;

import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.UUID;

public class CaseDetailsMapper {
    public CaseDetails transform(final String caseType, final UUID caseDetailsId, final JurisdictionType jurisdictionType) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(caseDetailsId.toString());
        caseDetails.set_case_type(caseType);
        if (CROWN.equals(jurisdictionType)) {
            caseDetails.set_is_crown(true);
            caseDetails.set_is_magistrates(false);
        }
        if (MAGISTRATES.equals(jurisdictionType)) {
            caseDetails.set_is_magistrates(true);
            caseDetails.set_is_crown(false);
        }
        return caseDetails;
    }
}
