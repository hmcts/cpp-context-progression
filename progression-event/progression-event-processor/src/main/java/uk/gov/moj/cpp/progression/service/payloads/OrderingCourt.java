package uk.gov.moj.cpp.progression.service.payloads;

public class OrderingCourt {

    private final String ljaCode;

    private final String ljaName;

    private final String courtCentreName;

    public OrderingCourt(String ljaCode, String ljaName, String courtCentreName) {
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.courtCentreName = courtCentreName;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    @Override
    public String toString() {
        return "OrderingCourt{" +
                "ljaCode='" + ljaCode + '\'' +
                ", ljaName='" + ljaName + '\'' +
                ", courtCentreName='" + courtCentreName + '\'' +
                '}';
    }

    public static OrderingCourt.Builder builder() {
        return new OrderingCourt.Builder();
    }


    public static class Builder {


        private String ljaCode;

        private String ljaName;

        private String courtCentreName;


        public OrderingCourt.Builder withLjaCode(final String ljaCode) {
            this.ljaCode = ljaCode;
            return this;
        }

        public OrderingCourt.Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }


        public OrderingCourt.Builder withCourtCenterName(final String courtCenterName) {
            this.courtCentreName = courtCenterName;
            return this;
        }

        public OrderingCourt build() {
            return new OrderingCourt(ljaCode, ljaName, courtCentreName);
        }

    }
}