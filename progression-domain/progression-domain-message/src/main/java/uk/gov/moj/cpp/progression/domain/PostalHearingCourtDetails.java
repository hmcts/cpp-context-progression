package uk.gov.moj.cpp.progression.domain;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class PostalHearingCourtDetails {

    private String courtName;
    private String courtNameWelsh;
    private String courtroomName;
    private String courtroomNameWelsh;
    private String hearingDate;
    private String hearingTime;
    private PostalAddress courtAddress;
    private PostalAddress courtAddressWelsh;

    public PostalHearingCourtDetails(final String courtName, final String courtNameWelsh, final String courtroomName, final String courtroomNameWelsh,  final String hearingDate, final String hearingTime, final PostalAddress courtAddress, final PostalAddress courtAddressWelsh) {
        this.courtName = courtName;
        this.courtNameWelsh = courtNameWelsh;
        this.courtroomName = courtroomName;
        this.courtroomNameWelsh = courtroomNameWelsh;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
        this.courtAddress = courtAddress;
        this.courtAddressWelsh = courtAddressWelsh;
    }

    public String getCourtroomName() {
        return courtroomName;
    }

    public String getCourtroomNameWelsh() {
        return courtroomNameWelsh;
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

    public PostalAddress getCourtAddressWelsh() {
        return courtAddressWelsh;
    }

    @Override
    public String toString() {
        return "PostalHearingCourtDetails{" +
                "courtName='" + courtName + '\'' +
                ", courtNameWelsh=" + courtNameWelsh +
                "courtroomName='" + courtroomName + '\'' +
                ", courtroomNameWelsh=" + courtroomNameWelsh +
                ", hearingDate=" + hearingDate +
                ", hearingTime=" + hearingTime +
                ", courtAddress=" + courtAddress +
                ", courtAddressWelsh=" + courtAddressWelsh +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String courtName;

        private String courtNameWelsh;

        private String courtroomName;

        private String courtroomNameWelsh;

        private String hearingDate;

        private String hearingTime;

        private PostalAddress courtAddress;

        private PostalAddress courtAddressWelsh;

        public Builder withCourtroomName(final String courtroomName) {
            this.courtroomName = courtroomName;
            return this;
        }

        public Builder withCourtroomNameWelsh(final String courtroomNameWelsh) {
            this.courtroomNameWelsh = courtroomNameWelsh;
            return this;
        }

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

        public Builder withCourtAddressWelsh(final PostalAddress courtAddressWelsh) {
            this.courtAddressWelsh = courtAddressWelsh;
            return this;
        }

        public PostalHearingCourtDetails build() {
            return new PostalHearingCourtDetails(courtName, courtNameWelsh, courtroomName, courtroomNameWelsh, hearingDate, hearingTime, courtAddress, courtAddressWelsh);
        }

    }
}
