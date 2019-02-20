package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class AncillaryOrders {
    private String details;

    private Boolean isAncillaryOrders;

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public Boolean getIsAncillaryOrders() {
        return isAncillaryOrders;
    }

    public void setIsAncillaryOrders(final Boolean isAncillaryOrders) {
        this.isAncillaryOrders = isAncillaryOrders;
    }

    public AncillaryOrders(final String details, final Boolean isAncillaryOrders) {
        super();
        this.details = details;
        this.isAncillaryOrders = isAncillaryOrders;
    }

    public AncillaryOrders() {
        super();
    }
}
