package uk.gov.moj.cpp.progression.command.defendant;

public class ProbationCommand {

    private String provideGuidance;
    private Boolean drugAssessment;
    private Boolean dangerousnessAssessment;

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

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public void setDangerousnessAssessment(Boolean dangerousnessAssessment) {
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    @Override
    public String toString() {
        return "ProbationCommand{" + "provideGuidance=" + provideGuidance + ", dangerousnessAssessment="
                + dangerousnessAssessment + ", drugAssessment=" + drugAssessment + '}';
    }
}