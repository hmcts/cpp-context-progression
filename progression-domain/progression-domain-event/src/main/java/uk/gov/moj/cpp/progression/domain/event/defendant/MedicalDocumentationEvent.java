package uk.gov.moj.cpp.progression.domain.event.defendant;

public class MedicalDocumentationEvent {
    private final String details;
    private final Boolean isMedicalDocumentation;

    private MedicalDocumentationEvent(Boolean isMedicalDocumentation, String details) {
        this.details = details;
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    public String getDetails() {
        return details;
    }

    public Boolean getIsMedicalDocumentation() {
        return isMedicalDocumentation;
    }

    public static final class MedicalDocumentationBuilder {
        private String details;
        private Boolean isMedicalDocumentation;

        private MedicalDocumentationBuilder() {
        }

        public static MedicalDocumentationEvent.MedicalDocumentationBuilder aMedicalDocumentationEvent() {
            return new MedicalDocumentationEvent.MedicalDocumentationBuilder();
        }

        public MedicalDocumentationEvent.MedicalDocumentationBuilder setDetails(String details) {
            this.details = details;
            return this;
        }

        public MedicalDocumentationEvent.MedicalDocumentationBuilder setIsMedicalDocumentation(
                Boolean isMedicalDocumentation) {
            this.isMedicalDocumentation = isMedicalDocumentation;
            return this;
        }

        public MedicalDocumentationEvent build() {
            MedicalDocumentationEvent medicalDocumentationEvent = new MedicalDocumentationEvent(isMedicalDocumentation,
                    details);
            return medicalDocumentationEvent;
        }
    }

}