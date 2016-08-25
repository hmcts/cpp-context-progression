package uk.gov.moj.cpp.progression.query.view.response;

public class Probation {
    private Boolean dangerousnessAssessment;

    private PreSentenceReport preSentenceReport;

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public void setDangerousnessAssessment(Boolean dangerousnessAssessment) {
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public PreSentenceReport getPreSentenceReport() {
        return preSentenceReport;
    }

    public void setPreSentenceReport(PreSentenceReport preSentenceReport) {
        this.preSentenceReport = preSentenceReport;
    }

    public Probation(Boolean dangerousnessAssessment, PreSentenceReport preSentenceReport) {
        super();
        this.dangerousnessAssessment = dangerousnessAssessment;
        this.preSentenceReport = preSentenceReport;
    }

    public Probation() {
        super();
    }

}
