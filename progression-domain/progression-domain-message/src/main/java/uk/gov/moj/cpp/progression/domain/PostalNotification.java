package uk.gov.moj.cpp.progression.domain;

import java.time.LocalDate;

public class PostalNotification {

    private final String reference;

    private final String ljaCode;

    private final String ljaName;

    private final LocalDate issueDate;

    private final PostalDefendant defendant;

    private final PostalAddressee addressee;

    private final String applicationType;

    private final String legislationText;

    private final String courtCentreName;

    private PostalHearingCourtDetails hearingCourtDetails;

    @SuppressWarnings({"squid:S00107"})
    public PostalNotification(final String reference, final String ljaCode, final String ljaName, final LocalDate issueDate, final PostalDefendant defendant, final PostalAddressee addressee, final String applicationType, final String legislationText, final String courtCentreName, final PostalHearingCourtDetails hearingCourtDetails) {
        this.reference = reference;
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.issueDate = issueDate;
        this.defendant = defendant;
        this.addressee = addressee;
        this.applicationType = applicationType;
        this.legislationText = legislationText;
        this.courtCentreName = courtCentreName;
        this.hearingCourtDetails = hearingCourtDetails;
    }

    public String getReference() {
        return reference;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public PostalDefendant getDefendant() {
        return defendant;
    }

    public PostalAddressee getAddressee() {
        return addressee;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public PostalHearingCourtDetails getHearingCourtDetails() {
        return hearingCourtDetails;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public String getLegislationText() {
        return legislationText;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "PostalNotification{" +
                "reference='" + reference + '\'' +
                ", ljaCode='" + ljaCode + '\'' +
                ", ljaName='" + ljaName + '\'' +
                ", issueDate=" + issueDate +
                ", defendant=" + defendant +
                ", addressee=" + addressee +
                ", applicationType='" + applicationType + '\'' +
                ", legislationText='" + legislationText + '\'' +
                ", courtCentreName='" + courtCentreName + '\'' +
                ", hearingCourtDetails=" + hearingCourtDetails +
                '}';
    }

    public static class Builder {

        private String reference;

        private String ljaCode;

        private String ljaName;

        private LocalDate issueDate;

        private PostalDefendant defendant;

        private PostalAddressee addressee;

        private String courtCentreName;

        private PostalHearingCourtDetails hearingCourtDetails;

        private String applicationType;

        private String legislationText;

        public Builder withReference(final String reference) {
            this.reference = reference;
            return this;
        }

        public Builder withLjaCode(final String ljaCode) {
            this.ljaCode = ljaCode;
            return this;
        }

        public Builder withLjaName(final String ljaName) {
            this.ljaName = ljaName;
            return this;
        }

        public Builder withIssueDate(final LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder withDefendant(final PostalDefendant defendant) {
            this.defendant = defendant;
            return this;
        }

        public Builder withAddressee(final PostalAddressee addressee) {
            this.addressee = addressee;
            return this;
        }

        public Builder withCourtCentreName(final String courtCentreName) {
            this.courtCentreName = courtCentreName;
            return this;
        }

        public Builder withHearingCourtDetails(final PostalHearingCourtDetails hearingCourtDetails) {
            this.hearingCourtDetails = hearingCourtDetails;
            return this;
        }

        public Builder withApplicationType(final String applicationType) {
            this.applicationType = applicationType;
            return this;
        }

        public Builder withLegislationText(final String legislationText) {
            this.legislationText = legislationText;
            return this;
        }

        public PostalNotification build() {
            return new PostalNotification(reference, ljaCode, ljaName, issueDate, defendant, addressee, applicationType, legislationText, courtCentreName, hearingCourtDetails);
        }
    }
}
