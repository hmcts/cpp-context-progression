package uk.gov.moj.cpp.progression.command.defendant;

public class PreSentenceReportCommand {
    private  String provideGuidance;
    private  Boolean drugAssessment;

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
        return "PreSentenceReportCommand{" +
                "provideGuidance='" + provideGuidance + '\'' +
                ", drugAssessment=" + drugAssessment +
                '}';
    }
}