package uk.gov.moj.cpp.progression.command.defendant;

public class ProsecutionCommand {

    private  AncillaryOrdersCommand ancillaryOrders;
    private  OthersCommand others;

    public AncillaryOrdersCommand getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(AncillaryOrdersCommand ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public OthersCommand getOthers() {
        return others;
    }

    public void setOthers(OthersCommand others) {
        this.others = others;
    }

    @Override
    public String toString() {
        return "ProsecutionCommand{" +
                "ancillaryOrders=" + ancillaryOrders +
                ", others=" + others +
                '}';
    }
}