package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class Probation {
    private Boolean dangerousnessAssessment;

    private PreSentenceReport preSentenceReport;

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public void setDangerousnessAssessment(final Boolean dangerousnessAssessment) {
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public PreSentenceReport getPreSentenceReport() {
        return preSentenceReport;
    }

    public void setPreSentenceReport(final PreSentenceReport preSentenceReport) {
        this.preSentenceReport = preSentenceReport;
    }

    public Probation(final Boolean dangerousnessAssessment, final PreSentenceReport preSentenceReport) {
        super();
        this.dangerousnessAssessment = dangerousnessAssessment;
        this.preSentenceReport = preSentenceReport;
    }

    public Probation() {
        super();
    }

}
