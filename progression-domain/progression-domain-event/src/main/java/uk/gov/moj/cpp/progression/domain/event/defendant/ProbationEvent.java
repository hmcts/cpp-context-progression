package uk.gov.moj.cpp.progression.domain.event.defendant;

public class ProbationEvent {

    private final String provideGuidance;
    private final Boolean drugAssessment;
    private final Boolean dangerousnessAssessment;

    private ProbationEvent(String provideGuidance, Boolean drugAssessment, Boolean dangerousnessAssessment) {
        this.provideGuidance = provideGuidance;
        this.drugAssessment = drugAssessment;
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public static final class ProbationEventBuilder {
        private String provideGuidance;
        private Boolean drugAssessment;
        private Boolean dangerousnessAssessment;

        private ProbationEventBuilder() {
        }

        public static ProbationEventBuilder aProbationEvent() {
            return new ProbationEventBuilder();
        }

        public ProbationEventBuilder provideGuidance(String provideGuidance) {
            this.provideGuidance = provideGuidance;
            return this;
        }

        public ProbationEventBuilder drugAssessment(Boolean drugAssessment) {
            this.drugAssessment = drugAssessment;
            return this;
        }

        public ProbationEventBuilder dangerousnessAssessment(Boolean dangerousnessAssessment) {
            this.dangerousnessAssessment = dangerousnessAssessment;
            return this;
        }

        public ProbationEvent build() {
            return new ProbationEvent(provideGuidance, drugAssessment, dangerousnessAssessment);
        }
    }
}