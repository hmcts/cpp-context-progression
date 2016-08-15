package uk.gov.moj.cpp.progression.command.defendant;

public class PreSentenceReportCommand {
    private final String provideGuidance;
    private final Boolean drugAssessment;

    private PreSentenceReportCommand(String provideGuidance, Boolean drugAssessment) {
        this.provideGuidance = provideGuidance;
        this.drugAssessment = drugAssessment;
    }

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public static final class PreSentenceReportCommandBuilder {
        private String provideGuidance;
        private Boolean drugAssessment;

        private PreSentenceReportCommandBuilder() {
        }

        public static PreSentenceReportCommandBuilder aPreSentenceReportCommand() {
            return new PreSentenceReportCommandBuilder();
        }

        public PreSentenceReportCommandBuilder provideGuidance(String provideGuidance) {
            this.provideGuidance = provideGuidance;
            return this;
        }

        public PreSentenceReportCommandBuilder drugAssessment(Boolean drugAssessment) {
            this.drugAssessment = drugAssessment;
            return this;
        }

        public PreSentenceReportCommand build() {
            return new PreSentenceReportCommand(provideGuidance, drugAssessment);
        }
    }
}