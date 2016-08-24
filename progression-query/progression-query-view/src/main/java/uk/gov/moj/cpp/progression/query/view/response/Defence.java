package uk.gov.moj.cpp.progression.query.view.response;

public class Defence {
    private String otherDetails;

    private StatementOfMeans statementOfMeans;

    private MedicalDocumentation medicalDocumentation;

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(String otherDetails) {
        this.otherDetails = otherDetails;
    }

    public StatementOfMeans getStatementOfMeans() {
        return statementOfMeans;
    }

    public void setStatementOfMeans(StatementOfMeans statementOfMeans) {
        this.statementOfMeans = statementOfMeans;
    }

    public MedicalDocumentation getMedicalDocumentation() {
        return medicalDocumentation;
    }

    public void setMedicalDocumentation(MedicalDocumentation medicalDocumentation) {
        this.medicalDocumentation = medicalDocumentation;
    }

    public Defence(String otherDetails, StatementOfMeans statementOfMeans,
                    MedicalDocumentation medicalDocumentation) {
        super();
        this.otherDetails = otherDetails;
        this.statementOfMeans = statementOfMeans;
        this.medicalDocumentation = medicalDocumentation;
    }

    public Defence() {
        super();
    }

}
