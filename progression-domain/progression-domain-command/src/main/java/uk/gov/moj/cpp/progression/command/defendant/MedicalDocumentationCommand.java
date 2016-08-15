package uk.gov.moj.cpp.progression.command.defendant;


public class MedicalDocumentationCommand {
    private final String details;

    private MedicalDocumentationCommand(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public static final class MedicalDocumentationCommandBuilder {
        private String details;

        private MedicalDocumentationCommandBuilder() {
        }

        public static MedicalDocumentationCommandBuilder aMedicalDocumentationCommand() {
            return new MedicalDocumentationCommandBuilder();
        }

        public MedicalDocumentationCommandBuilder details(String details) {
            this.details = details;
            return this;
        }

        public MedicalDocumentationCommand build() {
            MedicalDocumentationCommand medicalDocumentation = new MedicalDocumentationCommand(details);
            return medicalDocumentation;
        }
    }

}