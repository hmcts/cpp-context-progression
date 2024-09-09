package uk.gov.moj.cpp.progression.domain;

import java.time.LocalDate;

@SuppressWarnings("pmd:BeanMembersShouldSerialize")
public class PostalNotification {

    private final String reference;

    private final String ljaCode;

    private final String ljaName;

    private final String ljaNameWelsh;

    private final LocalDate issueDate;

    private final String amendmentDate;

    private final PostalDefendant defendant;

    private final PostalAddressee addressee;

    private final String applicationType;

    private final String applicationTypeWelsh;

    private final String legislationText;

    private final String legislationTextWelsh;

    private final String courtCentreName;

    private final String courtCentreNameWelsh;

    private final PostalHearingCourtDetails hearingCourtDetails;

    private final String applicationParticulars;

    private final String applicantName;

    private final String thirdParty;

    private final Boolean isWelsh;

    private final Boolean isAmended;

    @SuppressWarnings({"squid:S00107"})
    public PostalNotification(final String reference, final String ljaCode, final String ljaName, final String ljaNameWelsh, final LocalDate issueDate, final PostalDefendant defendant, final PostalAddressee addressee, final String applicationType, final String applicationTypeWelsh, final String legislationText, final String legislationTextWelsh, final String courtCentreName, final String courtCentreNameWelsh, final PostalHearingCourtDetails hearingCourtDetails, final String applicationParticulars, final String applicantName, final String thirdParty, final Boolean isWelsh, final Boolean isAmended, final String amendmentDate) {
        this.reference = reference;
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.ljaNameWelsh = ljaNameWelsh;
        this.issueDate = issueDate;
        this.defendant = defendant;
        this.addressee = addressee;
        this.applicationType = applicationType;
        this.applicationTypeWelsh = applicationTypeWelsh;
        this.legislationText = legislationText;
        this.legislationTextWelsh = legislationTextWelsh;
        this.courtCentreName = courtCentreName;
        this.courtCentreNameWelsh = courtCentreNameWelsh;
        this.hearingCourtDetails = hearingCourtDetails;
        this.applicationParticulars = applicationParticulars;
        this.applicantName = applicantName;
        this.thirdParty = thirdParty;
        this.isWelsh = isWelsh;
        this.isAmended = isAmended;
        this.amendmentDate = amendmentDate;
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

    public String getLjaNameWelsh() {
        return ljaNameWelsh;
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

    public String getCourtCentreNameWelsh() {
        return courtCentreNameWelsh;
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

    public String getApplicationTypeWelsh() {
        return applicationTypeWelsh;
    }

    public String getLegislationTextWelsh() {
        return legislationTextWelsh;
    }

    public String getApplicationParticulars() {
        return applicationParticulars;
    }

    public String getThirdParty() {
        return thirdParty;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public Boolean getIsWelsh() {
        return isWelsh;
    }

    public Boolean getIsAmended() {
        return isAmended;
    }

    public String getAmendmentDate() {
        return amendmentDate;
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
                ", ljaNameWelsh='" + ljaNameWelsh + '\'' +
                ", issueDate=" + issueDate +
                ", defendant=" + defendant +
                ", addressee=" + addressee +
                ", applicationType='" + applicationType + '\'' +
                ", applicationTypeWelsh='" + applicationTypeWelsh + '\'' +
                ", legislationText='" + legislationText + '\'' +
                ", legislationTextWelsh='" + legislationTextWelsh + '\'' +
                ", courtCentreName='" + courtCentreName + '\'' +
                ", courtCentreNameWelsh='" + courtCentreNameWelsh + '\'' +
                ", hearingCourtDetails=" + hearingCourtDetails + '\'' +
                ", applicationParticulars=" + applicationParticulars + '\'' +
                ", applicantName" + applicantName + '\'' +
                ", thirdParty=" + thirdParty +
                ", isWelsh=" + isWelsh +
                ", isAmended=" + isAmended +
                ", amendmentDate=" + amendmentDate +
                '}';
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private String reference;

        private String ljaCode;

        private String ljaName;

        private String ljaNameWelsh;

        private LocalDate issueDate;

        private String amendmentDate;

        private PostalDefendant defendant;

        private PostalAddressee addressee;

        private String courtCentreName;

        private String courtCentreNameWelsh;

        private PostalHearingCourtDetails hearingCourtDetails;

        private String applicationType;

        private String applicationTypeWelsh;

        private String legislationText;

        private String legislationTextWelsh;

        private String applicationParticulars;

        private String applicantName;

        private String thirdParty;

        private Boolean isWelsh;

        private Boolean isAmended;


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

        public Builder withLjaNameWelsh(final String ljaNameWelsh) {
            this.ljaNameWelsh = ljaNameWelsh;
            return this;
        }
        public Builder withIssueDate(final LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder withAmendmentDate(final String amendmentDate) {
            this.amendmentDate = amendmentDate;
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

        public Builder withCourtCentreNameWelsh(final String courtCentreNameWelsh) {
            this.courtCentreNameWelsh = courtCentreNameWelsh;
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

        public Builder withApplicationTypeWelsh(final String welshApplicationType) {
            this.applicationTypeWelsh = welshApplicationType;
            return this;
        }

        public Builder withLegislationText(final String legislationText) {
            this.legislationText = legislationText;
            return this;
        }

        public Builder withLegislationTextWelsh(final String welshLegislationText) {
            this.legislationTextWelsh = welshLegislationText;
            return this;
        }

        public Builder withApplicationParticulars(final String applicationParticulars) {
            this.applicationParticulars = applicationParticulars;
            return this;
        }

        public Builder withThirdParty(final String thirdParty) {
            this.thirdParty = thirdParty;
            return this;
        }

        public Builder withApplicantName(final String applicantName) {
            this.applicantName = applicantName;
            return this;
        }

        public Builder withIsWelsh(final Boolean isWelsh) {
            this.isWelsh = isWelsh;
            return this;
        }

        public Builder withIsAmended(final Boolean isAmended) {
            this.isAmended = isAmended;
            return this;
        }

        public PostalNotification build() {
            return new PostalNotification(reference, ljaCode, ljaName,ljaNameWelsh, issueDate, defendant, addressee, applicationType, applicationTypeWelsh, legislationText, legislationTextWelsh, courtCentreName, courtCentreNameWelsh, hearingCourtDetails, applicationParticulars, applicantName, thirdParty, isWelsh, isAmended, amendmentDate);
        }
    }
}
