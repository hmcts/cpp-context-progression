package uk.gov.justice.services;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.UUID;

public class CaseDetailsMapper {
    public CaseDetails transform(final String caseType, final UUID caseDetailsId, final JurisdictionType jurisdictionType) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(caseDetailsId.toString());
        caseDetails.set_case_type(caseType);
        caseDetails.setCaseStatus("ACTIVE");
        if (jurisdictionType.equals(JurisdictionType.CROWN)) {
            caseDetails.set_is_crown(true);
            caseDetails.set_is_magistrates(false);
            caseDetails.set_is_charging(false);
            caseDetails.set_is_sjp(false);
        }
        if (jurisdictionType.equals(JurisdictionType.MAGISTRATES)) {
            caseDetails.set_is_magistrates(true);
            caseDetails.set_is_crown(false);
            caseDetails.set_is_charging(false);
            caseDetails.set_is_sjp(false);
        }
        return caseDetails;
    }
}
