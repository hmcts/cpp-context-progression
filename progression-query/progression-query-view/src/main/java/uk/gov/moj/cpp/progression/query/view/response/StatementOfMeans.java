package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class StatementOfMeans {

    public StatementOfMeans() {
        super();
    }

    public StatementOfMeans(final String details, final Boolean isStatementOfMeans) {
        super();
        this.details = details;
        this.isStatementOfMeans = isStatementOfMeans;
    }

    private String details;

    private Boolean isStatementOfMeans;

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public Boolean getIsStatementOfMeans() {
        return isStatementOfMeans;
    }


    public void setIsStatementOfMeans(final Boolean isStatementOfMeans) {
        this.isStatementOfMeans = isStatementOfMeans;
    }
}
