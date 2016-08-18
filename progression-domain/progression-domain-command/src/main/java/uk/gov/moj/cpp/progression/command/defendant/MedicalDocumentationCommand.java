package uk.gov.moj.cpp.progression.command.defendant;

public class MedicalDocumentationCommand {
    private String details;
    private Boolean isMedicalDocumentation;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getIsMedicalDocumentation() {
        return isMedicalDocumentation;
    }

    public void setIsMedicalDocumentation(Boolean isMedicalDocumentation) {
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    @Override
    public String toString() {
        return "MedicalDocumentationCommand{" + "details='" + details + '\'' + "isMedicalDocumentation='"
                + isMedicalDocumentation + '\'' + '}';
    }
}