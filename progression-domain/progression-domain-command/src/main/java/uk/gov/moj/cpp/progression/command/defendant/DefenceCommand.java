package uk.gov.moj.cpp.progression.command.defendant;

public class DefenceCommand {
    private String statementOfMeans;
    private String medicalDocumentation;
    private String others;

    public String getStatementOfMeans() {
        return statementOfMeans;
    }

    public void setStatementOfMeans(String statementOfMeans) {
        this.statementOfMeans = statementOfMeans;
    }

    public String getMedicalDocumentation() {
        return medicalDocumentation;
    }

    public void setMedicalDocumentation(String medicalDocumentation) {
        this.medicalDocumentation = medicalDocumentation;
    }

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    @Override
    public String toString() {
        return "DefenceCommand{" + "statementOfMeans=" + statementOfMeans + ", medicalDocumentation="
                + medicalDocumentation + ", others=" + others + '}';
    }
}