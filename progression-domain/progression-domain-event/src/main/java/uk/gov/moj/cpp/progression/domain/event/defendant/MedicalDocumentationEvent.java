package uk.gov.moj.cpp.progression.domain.event.defendant;


public class MedicalDocumentationEvent {
    private final String details;

    private MedicalDocumentationEvent(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class MedicalDocumentationBuilder {
        private String details;

        private MedicalDocumentationBuilder() {
        }

        public static MedicalDocumentationEvent.MedicalDocumentationBuilder aMedicalDocumentationEvent() {
            return new MedicalDocumentationEvent.MedicalDocumentationBuilder();
        }

        public MedicalDocumentationEvent.MedicalDocumentationBuilder details(String details) {
            this.details = details;
            return this;
        }

        public MedicalDocumentationEvent build() {
            MedicalDocumentationEvent medicalDocumentationEvent = new MedicalDocumentationEvent(details);
            return medicalDocumentationEvent;
        }
    }

}