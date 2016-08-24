package uk.gov.moj.cpp.progression.query.view.response;

public class StatementOfMeans {

    public StatementOfMeans() {
        super();
    }

    public StatementOfMeans(String details, Boolean isStatementOfMeans) {
        super();
        this.details = details;
        this.isStatementOfMeans = isStatementOfMeans;
    }

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
}
