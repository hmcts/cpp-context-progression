package uk.gov.moj.cpp.progression.domain.event.defendant;

public class PreSentenceReportEvent {
    private final String provideGuidance;
    private final Boolean drugAssessment;

    private PreSentenceReportEvent(String provideGuidance, Boolean drugAssessment) {
        this.provideGuidance = provideGuidance;
        this.drugAssessment = drugAssessment;
    }

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public static final class PreSentenceReportEventBuilder {
        private String provideGuidance;
        private Boolean drugAssessment;

        private PreSentenceReportEventBuilder() {
        }

        public static PreSentenceReportEventBuilder aPreSentenceReportEvent() {
            return new PreSentenceReportEventBuilder();
        }

        public PreSentenceReportEventBuilder provideGuidance(String provideGuidance) {
            this.provideGuidance = provideGuidance;
            return this;
        }

        public PreSentenceReportEventBuilder drugAssessment(Boolean drugAssessment) {
            this.drugAssessment = drugAssessment;
            return this;
        }

        public PreSentenceReportEvent build() {
            return new PreSentenceReportEvent(provideGuidance, drugAssessment);
        }
    }
}