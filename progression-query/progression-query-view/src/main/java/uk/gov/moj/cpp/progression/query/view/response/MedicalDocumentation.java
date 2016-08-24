package uk.gov.moj.cpp.progression.query.view.response;

public class MedicalDocumentation {
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

    public MedicalDocumentation(String details, Boolean isMedicalDocumentation) {
        super();
        this.details = details;
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    public MedicalDocumentation() {
        super();
    }
}
