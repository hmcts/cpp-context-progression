package uk.gov.moj.cpp.progression.domain.event.defendant;

public class DefenceEvent {
    private final String statementOfMeans;
    private final String medicalDocumentation;
    private final String others;

    public DefenceEvent(String statementOfMeans, String medicalDocumentation, String others) {
        this.statementOfMeans = statementOfMeans;
        this.medicalDocumentation = medicalDocumentation;
        this.others = others;
    }

    public String getStatementOfMeansEvent() {
        return statementOfMeans;
    }

    public String getMedicalDocumentationEvent() {
        return medicalDocumentation;
    }

    public String getOthersEvent() {
        return others;
    }

    public static final class DefenceEventBuilder {
        private String statementOfMeans;
        private String medicalDocumentation;
        private String others;

        private DefenceEventBuilder() {
        }

        public DefenceEventBuilder statementOfMeans(String statementOfMeansEvent) {
            this.statementOfMeans = statementOfMeansEvent;
            return this;
        }

        public DefenceEventBuilder medicalDocumentation(String medicalDocumentationEvent) {
            this.medicalDocumentation = medicalDocumentationEvent;
            return this;
        }

        public DefenceEventBuilder others(String others) {
            this.others = others;
            return this;
        }

        public static DefenceEventBuilder aDefenceEvent() {
            return new DefenceEventBuilder();
        }

        public DefenceEvent build() {
            return new DefenceEvent(statementOfMeans, medicalDocumentation, others);
        }
    }
}