package uk.gov.moj.cpp.progression.command.defendant;

public class DefenceCommand {
    private  StatementOfMeansCommand statementOfMeans;
    private  MedicalDocumentationCommand medicalDocumentation;
    private  OthersCommand others;

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

    public OthersCommand getOthers() {
        return others;
    }

    public void setOthers(OthersCommand others) {
        this.others = others;
    }

    @Override
    public String toString() {
        return "DefenceCommand{" +
                "statementOfMeans=" + statementOfMeans +
                ", medicalDocumentation=" + medicalDocumentation +
                ", others=" + others +
                '}';
    }
}