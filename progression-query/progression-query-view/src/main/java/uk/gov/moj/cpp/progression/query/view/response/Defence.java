package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class Defence {
    private String otherDetails;

    private StatementOfMeans statementOfMeans;

    private MedicalDocumentation medicalDocumentation;

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(final String otherDetails) {
        this.otherDetails = otherDetails;
    }

    public StatementOfMeans getStatementOfMeans() {
        return statementOfMeans;
    }

    public void setStatementOfMeans(final StatementOfMeans statementOfMeans) {
        this.statementOfMeans = statementOfMeans;
    }

    public MedicalDocumentation getMedicalDocumentation() {
        return medicalDocumentation;
    }

    public void setMedicalDocumentation(final MedicalDocumentation medicalDocumentation) {
        this.medicalDocumentation = medicalDocumentation;
    }

    public Defence(final String otherDetails, final StatementOfMeans statementOfMeans,
                    final MedicalDocumentation medicalDocumentation) {
        super();
        this.otherDetails = otherDetails;
        this.statementOfMeans = statementOfMeans;
        this.medicalDocumentation = medicalDocumentation;
    }

    public Defence() {
        super();
    }

}
