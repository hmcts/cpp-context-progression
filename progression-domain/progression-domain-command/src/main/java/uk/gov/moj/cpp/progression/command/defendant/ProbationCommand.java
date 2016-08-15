package uk.gov.moj.cpp.progression.command.defendant;

public class ProbationCommand {

    private final PreSentenceReportCommand preSentenceReport;
    private final Boolean dangerousnessAssessment;

    private ProbationCommand(PreSentenceReportCommand preSentenceReport, Boolean dangerousnessAssessment) {
        this.preSentenceReport = preSentenceReport;
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public PreSentenceReportCommand getPreSentenceReportCommand() {
        return preSentenceReport;
    }

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public static final class ProbationCommandBuilder {
        private PreSentenceReportCommand preSentenceReport;
        private Boolean dangerousnessAssessment;

        private ProbationCommandBuilder() {
        }

        public static ProbationCommandBuilder aProbationCommand() {
            return new ProbationCommandBuilder();
        }

        public ProbationCommandBuilder preSentenceReport(PreSentenceReportCommand preSentenceReport) {
            this.preSentenceReport = preSentenceReport;
            return this;
        }

        public ProbationCommandBuilder dangerousnessAssessment(Boolean dangerousnessAssessment) {
            this.dangerousnessAssessment = dangerousnessAssessment;
            return this;
        }

        public ProbationCommand build() {
            return new ProbationCommand(preSentenceReport, dangerousnessAssessment);
        }
    }
}