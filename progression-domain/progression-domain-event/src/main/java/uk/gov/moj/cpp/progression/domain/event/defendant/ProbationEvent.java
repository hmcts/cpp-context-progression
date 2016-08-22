package uk.gov.moj.cpp.progression.domain.event.defendant;

public class ProbationEvent {

    private final PreSentenceReportEvent preSentenceReportEvent;
    private final Boolean dangerousnessAssessment;

    private ProbationEvent(PreSentenceReportEvent preSentenceReportEvent, Boolean dangerousnessAssessment) {
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

        public ProbationEventBuilder preSentenceReport(PreSentenceReportEvent preSentenceReportEvent) {
            this.preSentenceReportEvent = preSentenceReportEvent;
            return this;
        }

        public ProbationEventBuilder dangerousnessAssessment(Boolean dangerousnessAssessment) {
            this.dangerousnessAssessment = dangerousnessAssessment;
            return this;
        }

        public ProbationEvent build() {
            return new ProbationEvent(preSentenceReportEvent, dangerousnessAssessment);
        }
    }
}