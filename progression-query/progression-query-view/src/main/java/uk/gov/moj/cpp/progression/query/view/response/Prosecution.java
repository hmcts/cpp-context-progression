package uk.gov.moj.cpp.progression.query.view.response;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class Prosecution {
    private String otherDetails;

    private AncillaryOrders ancillaryOrders;

    public String getOtherDetails() {
        return otherDetails;
    }

    public void setOtherDetails(final String otherDetails) {
        this.otherDetails = otherDetails;
    }

    public AncillaryOrders getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(final AncillaryOrders ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public Prosecution(final String otherDetails, final AncillaryOrders ancillaryOrders) {
        super();
        this.otherDetails = otherDetails;
        this.ancillaryOrders = ancillaryOrders;
    }

    public Prosecution() {
        super();
    }

}
