package uk.gov.moj.cpp.progression.query.view.response;

public class AncillaryOrders {
    private String details;

    private Boolean isAncillaryOrders;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getIsAncillaryOrders() {
        return isAncillaryOrders;
    }

    public void setIsAncillaryOrders(Boolean isAncillaryOrders) {
        this.isAncillaryOrders = isAncillaryOrders;
    }

    public AncillaryOrders(String details, Boolean isAncillaryOrders) {
        super();
        this.details = details;
        this.isAncillaryOrders = isAncillaryOrders;
    }

    public AncillaryOrders() {
        super();
    }
}
