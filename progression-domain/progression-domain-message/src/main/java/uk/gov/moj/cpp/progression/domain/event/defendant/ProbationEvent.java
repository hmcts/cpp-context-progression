package uk.gov.moj.cpp.progression.domain.event.defendant;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class ProbationEvent {

    private final PreSentenceReportEvent preSentenceReportEvent;
    private final Boolean dangerousnessAssessment;

    private ProbationEvent(final PreSentenceReportEvent preSentenceReportEvent, final Boolean dangerousnessAssessment) {
        this.preSentenceReportEvent = preSentenceReportEvent;
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public PreSentenceReportEvent getPreSentenceReportEvent() {
        return preSentenceReportEvent;
    }

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public static final class ProbationEventBuilder {
        private PreSentenceReportEvent preSentenceReportEvent;
        private Boolean dangerousnessAssessment;

        private ProbationEventBuilder() {
        }

        public static ProbationEventBuilder aProbationEvent() {
            return new ProbationEventBuilder();
        }

        public ProbationEventBuilder setPreSentenceReport(final PreSentenceReportEvent preSentenceReportEvent) {
            this.preSentenceReportEvent = preSentenceReportEvent;
            return this;
        }

        public ProbationEventBuilder setDangerousnessAssessment(final Boolean dangerousnessAssessment) {
            this.dangerousnessAssessment = dangerousnessAssessment;
            return this;
        }

        public ProbationEvent build() {
            return new ProbationEvent(preSentenceReportEvent, dangerousnessAssessment);
        }

        public PreSentenceReportEvent getPreSentenceReportEvent() {
            return preSentenceReportEvent;
        }

        public void setPreSentenceReportEvent(final PreSentenceReportEvent preSentenceReportEvent) {
            this.preSentenceReportEvent = preSentenceReportEvent;
        }

        public Boolean getDangerousnessAssessment() {
            return dangerousnessAssessment;
        }
        
        
    }
    
    
}