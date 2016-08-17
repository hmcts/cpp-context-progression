package uk.gov.moj.cpp.progression.command.defendant;

public class ProsecutionCommand {

    private String ancillaryOrders;
    private String others;

    public String getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(String ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    @Override
    public String toString() {
        return "ProsecutionCommand{" + "ancillaryOrders=" + ancillaryOrders + ", others=" + others + '}';
    }
}