package uk.gov.moj.cpp.progression.domain.event.defendant;

public class StatementOfMeansEvent {
    private final String details;

    private StatementOfMeansEvent(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class StatementOfMeansEventBuilder {
        private String details;

        private StatementOfMeansEventBuilder() {
        }

        public static StatementOfMeansEventBuilder aStatementOfMeansEvent() {
            return new StatementOfMeansEventBuilder();
        }

        public StatementOfMeansEventBuilder details(String details) {
            this.details = details;
            return this;
        }

        public StatementOfMeansEvent build() {
            return new StatementOfMeansEvent(details);
        }
    }
}