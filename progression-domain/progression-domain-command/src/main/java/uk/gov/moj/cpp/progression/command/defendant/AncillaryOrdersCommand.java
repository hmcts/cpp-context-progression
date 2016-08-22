package uk.gov.moj.cpp.progression.command.defendant;

public class AncillaryOrdersCommand {
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

    @Override
    public String toString() {
        return "AncillaryOrdersCommand{" + "details='" + details + '\'' + "isAncillaryOrders='" + isAncillaryOrders
                + '\'' + '}';
    }
}
