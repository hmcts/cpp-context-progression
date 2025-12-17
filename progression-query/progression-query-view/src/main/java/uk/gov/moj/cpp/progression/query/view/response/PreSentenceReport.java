package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class PreSentenceReport {
    private String provideGuidance;

    private Boolean psrIsRequested;

    private Boolean drugAssessment;

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public void setProvideGuidance(final String provideGuidance) {
        this.provideGuidance = provideGuidance;
    }

    public Boolean getPsrIsRequested() {
        return psrIsRequested;
    }

    public void setPsrIsRequested(final Boolean psrIsRequested) {
        this.psrIsRequested = psrIsRequested;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public void setDrugAssessment(final Boolean drugAssessment) {
        this.drugAssessment = drugAssessment;
    }

    public PreSentenceReport(final String provideGuidance, final Boolean psrIsRequested,
                    final Boolean drugAssessment) {
        super();
        this.provideGuidance = provideGuidance;
        this.psrIsRequested = psrIsRequested;
        this.drugAssessment = drugAssessment;
    }

    public PreSentenceReport() {
        super();
    }
}
