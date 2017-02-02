package uk.gov.moj.cpp.progression.domain.event.defendant;

public class StatementOfMeansEvent {
    private final String details;
    private final Boolean isStatementOfMeans;

    private StatementOfMeansEvent(Boolean isStatementOfMeans, String details) {
        this.details = details;
        this.isStatementOfMeans = isStatementOfMeans;
    }

    public String getDetails() {
        return details;
    }

    public Boolean getIsStatementOfMeans() {
        return isStatementOfMeans;
    }

    
    public static final class StatementOfMeansEventBuilder {
        private String details;
        private Boolean isStatementOfMeans;

        private StatementOfMeansEventBuilder() {
        }

        public static StatementOfMeansEventBuilder aStatementOfMeansEvent() {
            return new StatementOfMeansEventBuilder();
        }

        public StatementOfMeansEventBuilder setDetails(String details) {
            this.details = details;
            return this;
        }

        public StatementOfMeansEventBuilder setIsStatementOfMeans(Boolean isStatementOfMeans) {
            this.isStatementOfMeans = isStatementOfMeans;
            return this;
        }

        public StatementOfMeansEvent build() {
            return new StatementOfMeansEvent(isStatementOfMeans, details);
        }

        public String getDetails() {
            return details;
        }

        public Boolean getIsStatementOfMeans() {
            return isStatementOfMeans;
        }
        
        
    }
}