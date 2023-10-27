package uk.gov.moj.cpp.progression.service.payloads;

public class OrderingCourt {

    private final String ljaCode;

    private final String ljaName;

    private final String welshLjaName;

    private final String courtCentreName;

    private final String welshCourtCentreName;

    public OrderingCourt(String ljaCode, String ljaName,String welshLjaName, String courtCentreName, String welshCourtCentreName) {
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.courtCentreName = courtCentreName;
        this.welshCourtCentreName = welshCourtCentreName;
        this.welshLjaName = welshLjaName;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getWelshLjaName() {
        return welshLjaName;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getWelshCourtCentreName() {
        return welshCourtCentreName;
    }

    @Override
    public String toString() {
        return "OrderingCourt{" +
                "ljaCode='" + ljaCode + '\'' +
                ", ljaName='" + ljaName + '\'' +
                ", welshLjaName='" + welshLjaName + '\'' +
                ", courtCentreName='" + courtCentreName + '\'' +
                ", welshCourtCentreName='" + welshCourtCentreName + '\'' +
                '}';
    }

    public static OrderingCourt.Builder builder() {
        return new OrderingCourt.Builder();
    }


    public static class Builder {


        private String ljaCode;

        private String ljaName;

        private String welshLjaName;

        private String courtCentreName;

        private String welshCourtCentreName;


        public OrderingCourt.Builder withLjaCode(final String ljaCode) {
            this.ljaCode = ljaCode;
            return this;
        }

        public OrderingCourt.Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public OrderingCourt.Builder withWelshLjaName(final String welshLjaName) {
            this.welshLjaName = welshLjaName;
            return this;
        }

        public OrderingCourt.Builder withCourtCenterName(final String courtCenterName) {
            this.courtCentreName = courtCenterName;
            return this;
        }

        public OrderingCourt.Builder withWelshCourtCenterName(final String welshCourtCentreName) {
            this.welshCourtCentreName = welshCourtCentreName;
            return this;
        }

        public OrderingCourt build() {
            return new OrderingCourt(ljaCode, ljaName, welshLjaName, courtCentreName, welshCourtCentreName);
        }

    }
}