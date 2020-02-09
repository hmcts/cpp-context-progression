package uk.gov.justice.services.domain;

import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.List;

public class CaseDocuments {

    private List<CaseDetails> caseDetails;

    public CaseDocuments(final List<CaseDetails> caseDetails) {
        this.caseDetails = caseDetails;
    }

    public List<CaseDetails> getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(final List<CaseDetails> caseDetails) {
        this.caseDetails = caseDetails;
    }

    @Override
    @SuppressWarnings("squid:S00121")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CaseDocuments that = (CaseDocuments) o;

        return caseDetails != null ? caseDetails.equals(that.caseDetails) : that.caseDetails == null;
    }

    @Override
    public int hashCode() {
        return caseDetails != null ? caseDetails.hashCode() : 0;
    }

}
