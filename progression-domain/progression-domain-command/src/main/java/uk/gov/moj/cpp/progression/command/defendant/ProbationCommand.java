package uk.gov.moj.cpp.progression.command.defendant;

public class ProbationCommand {

    private PreSentenceReportCommand preSentenceReport;
    private Boolean dangerousnessAssessment;

    public PreSentenceReportCommand getPreSentenceReport() {
        return preSentenceReport;
    }

    public void setPreSentenceReport(PreSentenceReportCommand preSentenceReport) {
        this.preSentenceReport = preSentenceReport;
    }

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public void setDangerousnessAssessment(Boolean dangerousnessAssessment) {
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    @Override
    public String toString() {
        return "ProbationCommand{" + "preSentenceReport=" + preSentenceReport + ", dangerousnessAssessment="
                + dangerousnessAssessment + '}';
    }
}