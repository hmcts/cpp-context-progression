package uk.gov.moj.cpp.progression.command.defendant;

public class DefenceCommand {
    private StatementOfMeansCommand statementOfMeans;
    private MedicalDocumentationCommand medicalDocumentation;
    private String otherDetails;

    public StatementOfMeansCommand getStatementOfMeans() {
        return statementOfMeans;
    }

    public void setStatementOfMeans(StatementOfMeansCommand statementOfMeans) {
        this.statementOfMeans = statementOfMeans;
    }

    public MedicalDocumentationCommand getMedicalDocumentation() {
        return medicalDocumentation;
    }

    public void setMedicalDocumentation(MedicalDocumentationCommand medicalDocumentation) {
        this.medicalDocumentation = medicalDocumentation;
    }

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(String otherDetails) {
        this.otherDetails = otherDetails;
    }

    @Override
    public String toString() {
        return "DefenceCommand{" + "statementOfMeans=" + statementOfMeans + ", medicalDocumentation="
                + medicalDocumentation + ", otherDetails=" + otherDetails + '}';
    }
}