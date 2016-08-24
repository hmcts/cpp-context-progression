package uk.gov.moj.cpp.progression.query.view.response;

public class Prosecution {
    private String otherDetails;

    private AncillaryOrders ancillaryOrders;

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(String otherDetails) {
        this.otherDetails = otherDetails;
    }

    public AncillaryOrders getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(AncillaryOrders ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public Prosecution(String otherDetails, AncillaryOrders ancillaryOrders) {
        super();
        this.otherDetails = otherDetails;
        this.ancillaryOrders = ancillaryOrders;
    }

    public Prosecution() {
        super();
    }

}
