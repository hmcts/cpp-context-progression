package uk.gov.moj.cpp.progression.command.defendant;

public class PreSentenceReportCommand {
    private String provideGuidance;
    private Boolean drugAssessment;
    private Boolean psrIsRequested;

    public Boolean getPsrIsRequested() {
        return psrIsRequested;
    }

    public void setPsrIsRequested(Boolean psrIsRequested) {
        this.psrIsRequested = psrIsRequested;
    }

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public void setProvideGuidance(String provideGuidance) {
        this.provideGuidance = provideGuidance;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public void setDrugAssessment(Boolean drugAssessment) {
        this.drugAssessment = drugAssessment;
    }

    @Override
    public String toString() {
        return "PreSentenceReportCommand{" + "provideGuidance='" + provideGuidance + '\'' + ", drugAssessment="
                + drugAssessment + ", psrIsRequested=" + psrIsRequested + '}';
    }
}
