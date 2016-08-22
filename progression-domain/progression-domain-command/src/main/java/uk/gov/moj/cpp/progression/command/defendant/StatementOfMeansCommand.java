package uk.gov.moj.cpp.progression.command.defendant;

public class StatementOfMeansCommand {
    private String details;
    private Boolean isStatementOfMeans;
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getIsStatementOfMeans() {
        return isStatementOfMeans;
    }

    public void setIsStatementOfMeans(Boolean isStatementOfMeans) {
        this.isStatementOfMeans = isStatementOfMeans;
    }

    @Override
    public String toString() {
        return "StatementOfMeansCommand{" + "details='" + details + '\'' + "isStatementOfMeans='" + isStatementOfMeans
                + '\'' + '}';
    }
}
