package uk.gov.moj.cpp.progression.event.nows.order;

public class NextHearingCourtDetails {

    private final String courtName;
    private final String hearingDate;
    private final String hearingTime;
    private final Address courtAddress;

    public NextHearingCourtDetails(final String courtName, final String hearingDate, final String hearingTime, final Address courtAddress) {
        this.courtName = courtName;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
        this.courtAddress = courtAddress;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public String getHearingTime() {
        return hearingTime;
    }

    public Address getCourtAddress() {
        return courtAddress;
    }
}
