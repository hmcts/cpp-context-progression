package uk.gov.moj.cpp.progression.query.view.response;

public class PreSentenceReport {
    private String provideGuidance;

    private Boolean psrIsRequested;

    private Boolean drugAssessment;

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public void setProvideGuidance(String provideGuidance) {
        this.provideGuidance = provideGuidance;
    }

    public Boolean getPsrIsRequested() {
        return psrIsRequested;
    }

    public void setPsrIsRequested(Boolean psrIsRequested) {
        this.psrIsRequested = psrIsRequested;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public void setDrugAssessment(Boolean drugAssessment) {
        this.drugAssessment = drugAssessment;
    }

    public PreSentenceReport(String provideGuidance, Boolean psrIsRequested,
                    Boolean drugAssessment) {
        super();
        this.provideGuidance = provideGuidance;
        this.psrIsRequested = psrIsRequested;
        this.drugAssessment = drugAssessment;
    }

    public PreSentenceReport() {
        super();
    }
}
