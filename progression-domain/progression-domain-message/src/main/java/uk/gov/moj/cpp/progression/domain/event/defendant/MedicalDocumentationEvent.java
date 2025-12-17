package uk.gov.moj.cpp.progression.domain.event.defendant;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class MedicalDocumentationEvent {
    private final String details;
    private final Boolean isMedicalDocumentation;

    private MedicalDocumentationEvent(final Boolean isMedicalDocumentation, final String details) {
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
        private transient String details;
        private transient Boolean isMedicalDocumentation;

        private MedicalDocumentationBuilder() {
        }

        public static MedicalDocumentationEvent.MedicalDocumentationBuilder aMedicalDocumentationEvent() {
            return new MedicalDocumentationEvent.MedicalDocumentationBuilder();
        }

        public MedicalDocumentationEvent.MedicalDocumentationBuilder setDetails(final String details) {
            this.details = details;
            return this;
        }

        public MedicalDocumentationEvent.MedicalDocumentationBuilder setIsMedicalDocumentation(
                final Boolean isMedicalDocumentation) {
            this.isMedicalDocumentation = isMedicalDocumentation;
            return this;
        }

        public MedicalDocumentationEvent build() {
            return new MedicalDocumentationEvent(isMedicalDocumentation,
                    details);
        }
    }

}