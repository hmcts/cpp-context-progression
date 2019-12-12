package uk.gov.moj.cpp.progression.service.dto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WelshFieldDetails {

    private final String ljaName;
    private final String courtCentreName;

    private final String courtAddressLine1;
    private final String courtAddressLine2;
    private final String courtAddressLine3;
    private final String courtAddressLine4;
    private final String courtAddressLine5;

    private final String referalReason;
    private final String referalText;

    private final List<OffenceDetails> offencesDetails;

    private WelshFieldDetails(final WelshFieldDetails.Builder builder) {
        ljaName = builder.ljaName;
        courtCentreName = builder.courtCentreName;
        courtAddressLine1 = builder.courtAddressLine1;
        courtAddressLine2 = builder.courtAddressLine2;
        courtAddressLine3 = builder.courtAddressLine3;
        courtAddressLine4 = builder.courtAddressLine4;
        courtAddressLine5 = builder.courtAddressLine5;
        referalReason = builder.referalReason;
        referalText = builder.referalText;
        offencesDetails = builder.offencesDetails;

    }

    public static WelshFieldDetails.Builder newBuilder() {
        return new WelshFieldDetails.Builder();
    }

    public static WelshFieldDetails.Builder newBuilder(final WelshFieldDetails copy) {
        final WelshFieldDetails.Builder builder = new WelshFieldDetails.Builder();
        builder.ljaName = copy.getLjaName();
        builder.courtCentreName = copy.getCourtCentreName();
        builder.courtAddressLine1 = copy.getCourtAddressLine1();
        builder.courtAddressLine2 = copy.getCourtAddressLine2();
        builder.courtAddressLine3 = copy.getCourtAddressLine3();
        builder.courtAddressLine4 = copy.getCourtAddressLine4();
        builder.courtAddressLine5 = copy.getCourtAddressLine5();
        builder.referalReason = copy.getReferalReason();
        builder.referalText = copy.getReferalText();
        builder.offencesDetails = copy.getOffenceDetails();
        return builder;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getCourtAddressLine1() {
        return courtAddressLine1;
    }

    public String getCourtAddressLine2() {
        return courtAddressLine2;
    }

    public String getCourtAddressLine3() {
        return courtAddressLine3;
    }

    public String getCourtAddressLine4() {
        return courtAddressLine4;
    }

    public String getCourtAddressLine5() {
        return courtAddressLine5;
    }

    public String getReferalReason() {
        return referalReason;
    }

    public String getReferalText() {
        return referalText;
    }

    public List<OffenceDetails> getOffenceDetails() {
        return Collections.unmodifiableList(offencesDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ljaName,
                courtCentreName,
                courtAddressLine1,
                courtAddressLine2,
                courtAddressLine3,
                courtAddressLine4,
                courtAddressLine5,
                referalReason,
                referalText,
                offencesDetails);
    }

    @Override
    public String toString() {

        final StringBuilder offencesStringBuilder = new StringBuilder();
        offencesDetails.forEach(o -> offencesStringBuilder.append("[ offenceTitle=" + o.getOffenceTitle() + "offenceLegislation" + o.getOffenceLegislation() + "]"));

        return "WelshFieldDetails{" +
                ", LJAName='" + ljaName + '\'' +
                ", CourtCentreName='" + courtCentreName + '\'' +
                ", courtAddressLine1='" + courtAddressLine1 + '\'' +
                ", courtAddressLine2='" + courtAddressLine2 + '\'' +
                ", courtAddressLine3='" + courtAddressLine3 + '\'' +
                ", courtAddressLine4='" + courtAddressLine4 + '\'' +
                ", courtAddressLine5='" + courtAddressLine5 + '\'' +
                ", referalReason='" + referalReason + '\'' +
                ", referalText='" + referalText + '\'' +
                offencesStringBuilder.toString() +
                '}';

    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final WelshFieldDetails that = (WelshFieldDetails) o;
        final boolean isLJANameAndCourtCentreNameEqual = Objects.equals(ljaName, that.getLjaName()) &&
                Objects.equals(courtCentreName, that.getCourtCentreName());
        final boolean isFirstLinesDetailsEqual = Objects.equals(courtAddressLine1, that.getCourtAddressLine1()) &&
                Objects.equals(courtAddressLine2, that.getCourtAddressLine2()) &&
                Objects.equals(courtAddressLine3, that.getCourtAddressLine3());
        final boolean isLaterLinesDetailsEqual = Objects.equals(courtAddressLine4, that.getCourtAddressLine4()) &&
                Objects.equals(courtAddressLine5, that.getCourtAddressLine5());
        final boolean isAddressDetailsEqual = isFirstLinesDetailsEqual && isLaterLinesDetailsEqual;
        final boolean isOffenceAndReferalDetailsEqual = Objects.equals(offencesDetails, that.getOffenceDetails()) &&
                Objects.equals(referalReason, that.getReferalReason()) &&
                Objects.equals(referalText, that.getReferalText());
        return isLJANameAndCourtCentreNameEqual && isAddressDetailsEqual && isOffenceAndReferalDetailsEqual;

    }


    public static final class Builder {

        private String ljaName;
        private String courtCentreName;

        private List<OffenceDetails> offencesDetails;

        private String courtAddressLine1;
        private String courtAddressLine2;
        private String courtAddressLine3;
        private String courtAddressLine4;
        private String courtAddressLine5;

        private String referalReason;
        private String referalText;

        private Builder() {
        }

        public WelshFieldDetails.Builder withLJAName(final String val) {
            ljaName = val;
            return this;
        }

        public WelshFieldDetails.Builder withCourtCentreName(final String val) {
            courtCentreName = val;
            return this;
        }

        public WelshFieldDetails.Builder withCourtAddressLine1(final String val) {
            courtAddressLine1 = val;
            return this;
        }

        public WelshFieldDetails.Builder withCourtAddressLine2(final String val) {
            courtAddressLine2 = val;
            return this;
        }

        public WelshFieldDetails.Builder withCourtAddressLine3(final String val) {
            courtAddressLine3 = val;
            return this;
        }

        public WelshFieldDetails.Builder withCourtAddressLine4(final String val) {
            courtAddressLine4 = val;
            return this;
        }

        public WelshFieldDetails.Builder withCourtAddressLine5(final String val) {
            courtAddressLine5 = val;
            return this;
        }

        public WelshFieldDetails.Builder withReferalReason(final String val) {
            referalReason = val;
            return this;
        }

        public WelshFieldDetails.Builder withReferalText(final String val) {
            referalText = val;
            return this;
        }

        public WelshFieldDetails.Builder withOffencesDetails(final List<OffenceDetails> val) {
            offencesDetails = Collections.unmodifiableList(val);
            return this;
        }

        public WelshFieldDetails build() {
            return new WelshFieldDetails(this);
        }
    }

    public boolean checkAllWelshValuesPresent() {
        final boolean isLJANameAndCourtCentreNameEqual = isValidValue(this.getLjaName()) &&
                isValidValue(this.getCourtCentreName());
        final boolean isCourtAddressDetailsEqual = isValidValue(this.getCourtAddressLine1());
        final boolean isOffenceAndReferalValuesEqual = isValidValue(this.getReferalReason()) &&
                !getOffenceDetails().isEmpty();
        return isLJANameAndCourtCentreNameEqual && isCourtAddressDetailsEqual && isOffenceAndReferalValuesEqual;
    }

    private boolean isValidValue(String val) {
        return Objects.nonNull(val) && !val.isEmpty();
    }

}

