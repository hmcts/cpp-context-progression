package uk.gov.moj.cpp.progression.domain;

public class PostalHearingCourtDetails {

    private String courtName;
    private String courtNameWelsh;

    private String hearingDate;

    private String hearingTime;

    private PostalAddress courtAddress;

    public PostalHearingCourtDetails(final String courtName, final String courtNameWelsh, final String hearingDate, final String hearingTime, final PostalAddress courtAddress) {
        this.courtName = courtName;
        this.courtNameWelsh = courtNameWelsh;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
        this.courtAddress = courtAddress;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getCourtNameWelsh() {
        return courtNameWelsh;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public String getHearingTime() {
        return hearingTime;
    }

    public PostalAddress getCourtAddress() {
        return courtAddress;
    }

    @Override
    public String toString() {
        return "PostalHearingCourtDetails{" +
                "courtName='" + courtName + '\'' +
                ", courtNameWelsh=" + courtNameWelsh +
                ", hearingDate=" + hearingDate +
                ", hearingTime=" + hearingTime +
                ", courtAddress=" + courtAddress +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String courtName;

        private String courtNameWelsh;

        private String hearingDate;

        private String hearingTime;

        private PostalAddress courtAddress;

        public Builder withCourtName(final String courtName) {
            this.courtName = courtName;
            return this;
        }

        public Builder withCourtNameWelsh(final String courtNameWelsh) {
            this.courtNameWelsh = courtNameWelsh;
            return this;
        }

        public Builder withHearingDate(final String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withHearingTime(final String hearingTime) {
            this.hearingTime = hearingTime;
            return this;
        }

        public Builder withCourtAddress(final PostalAddress courtAddress) {
            this.courtAddress = courtAddress;
            return this;
        }

        public PostalHearingCourtDetails build() {
            return new PostalHearingCourtDetails(courtName, courtNameWelsh, hearingDate, hearingTime, courtAddress);
        }

    }
}
