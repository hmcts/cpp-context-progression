package uk.gov.moj.cpp.progression.domain.event.defendant;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class DefenceEvent {
    private final StatementOfMeansEvent statementOfMeansEvent;
    private final MedicalDocumentationEvent medicalDocumentationEvent;
    private final String otherDetails;

    public DefenceEvent(final StatementOfMeansEvent statementOfMeansEvent,
            final MedicalDocumentationEvent medicalDocumentationEvent, final String otherDetails) {
        this.statementOfMeansEvent = statementOfMeansEvent;
        this.medicalDocumentationEvent = medicalDocumentationEvent;
        this.otherDetails = otherDetails;
    }

    public StatementOfMeansEvent getStatementOfMeansEvent() {
        return statementOfMeansEvent;
    }

    public MedicalDocumentationEvent getMedicalDocumentationEvent() {
        return medicalDocumentationEvent;
    }

    public String getOtherDetails() {
        return otherDetails;
    }

    public static final class DefenceEventBuilder {
        private StatementOfMeansEvent statementOfMeansEvent;
        private MedicalDocumentationEvent medicalDocumentationEvent;
        private String otherDetails;

        private DefenceEventBuilder() {
        }

        public DefenceEventBuilder statementOfMeans(final StatementOfMeansEvent statementOfMeansEvent) {
            this.statementOfMeansEvent = statementOfMeansEvent;
            return this;
        }

        public DefenceEventBuilder medicalDocumentation(final MedicalDocumentationEvent medicalDocumentationEvent) {
            this.medicalDocumentationEvent = medicalDocumentationEvent;
            return this;
        }

        public DefenceEventBuilder setOtherDetails(final String otherDetails) {
            this.otherDetails = otherDetails;
            return this;
        }

        public static DefenceEventBuilder aDefenceEvent() {
            return new DefenceEventBuilder();
        }

        public DefenceEvent build() {
            return new DefenceEvent(statementOfMeansEvent, medicalDocumentationEvent, otherDetails);
        }
    }
}