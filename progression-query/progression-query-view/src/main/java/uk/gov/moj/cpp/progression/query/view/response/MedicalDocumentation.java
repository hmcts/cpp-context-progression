package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class MedicalDocumentation {
    private String details;

    private Boolean isMedicalDocumentation;

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public Boolean getIsMedicalDocumentation() {
        return isMedicalDocumentation;
    }

    public void setIsMedicalDocumentation(final Boolean isMedicalDocumentation) {
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    public MedicalDocumentation(final String details, final Boolean isMedicalDocumentation) {
        super();
        this.details = details;
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    public MedicalDocumentation() {
        super();
    }
}
