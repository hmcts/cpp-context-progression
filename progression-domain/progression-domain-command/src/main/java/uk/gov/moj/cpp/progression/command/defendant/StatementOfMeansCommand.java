package uk.gov.moj.cpp.progression.command.defendant;

public class StatementOfMeansCommand {
    private final String details;

    private StatementOfMeansCommand(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class StatementOfMeansCommandBuilder {
        private String details;

        private StatementOfMeansCommandBuilder() {
        }

        public static StatementOfMeansCommandBuilder aStatementOfMeansCommand() {
            return new StatementOfMeansCommandBuilder();
        }

        public StatementOfMeansCommandBuilder details(String details) {
            this.details = details;
            return this;
        }

        public StatementOfMeansCommand build() {
            StatementOfMeansCommand statementOfMeans = new StatementOfMeansCommand(details);
            return statementOfMeans;
        }
    }
}