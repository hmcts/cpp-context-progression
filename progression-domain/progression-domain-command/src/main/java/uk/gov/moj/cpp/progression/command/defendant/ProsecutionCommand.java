package uk.gov.moj.cpp.progression.command.defendant;

public class ProsecutionCommand {

    private AncillaryOrdersCommand ancillaryOrders;
    private String otherDetails;

    public AncillaryOrdersCommand getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(AncillaryOrdersCommand ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(String otherDetails) {
        this.otherDetails = otherDetails;
    }

    @Override
    public String toString() {
        return "ProsecutionCommand{" + "ancillaryOrders=" + ancillaryOrders + ", others=" + otherDetails + '}';
    }
}