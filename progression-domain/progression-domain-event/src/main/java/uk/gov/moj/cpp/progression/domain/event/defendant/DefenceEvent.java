package uk.gov.moj.cpp.progression.domain.event.defendant;

public class DefenceEvent {
    private final StatementOfMeansEvent statementOfMeansEvent;
    private final MedicalDocumentationEvent medicalDocumentationEvent;
    private OtherEvent others;

    public DefenceEvent(StatementOfMeansEvent statementOfMeansEvent, MedicalDocumentationEvent medicalDocumentationEvent, OtherEvent others) {
        this.statementOfMeansEvent = statementOfMeansEvent;
        this.medicalDocumentationEvent = medicalDocumentationEvent;
        this.others = others;
    }

    public StatementOfMeansEvent getStatementOfMeansEvent() {
        return statementOfMeansEvent;
    }

    public MedicalDocumentationEvent getMedicalDocumentationEvent() {
        return medicalDocumentationEvent;
    }

    public OtherEvent getOthersEvent() {
        return others;
    }

    public static final class DefenceEventBuilder {
        private StatementOfMeansEvent statementOfMeansEvent;
        private MedicalDocumentationEvent medicalDocumentationEvent;
        private OtherEvent others;

        private DefenceEventBuilder() {
        }

        public DefenceEventBuilder statementOfMeans(StatementOfMeansEvent statementOfMeansEvent) {
            this.statementOfMeansEvent = statementOfMeansEvent;
            return this;
        }

        public DefenceEventBuilder medicalDocumentation(MedicalDocumentationEvent medicalDocumentationEvent) {
            this.medicalDocumentationEvent = medicalDocumentationEvent;
            return this;
        }

        public DefenceEventBuilder others(OtherEvent others) {
            this.others = others;
            return this;
        }

        public static DefenceEventBuilder aDefenceEvent() {
            return new DefenceEventBuilder();
        }

        public DefenceEvent build() {
            return new DefenceEvent(statementOfMeansEvent, medicalDocumentationEvent, others);
        }
    }
}