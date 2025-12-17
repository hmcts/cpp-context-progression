package uk.gov.moj.cpp.progression.service.payloads;


public class StatDecAppointmentLetterDefendantAddress {

    private final String line1;

    private final String line2;

    private final String line3;

    private final String line4;

    private final String line5;

    private final String postCode;

    public StatDecAppointmentLetterDefendantAddress(String line1, String line2, String line3, String line4, String line5, String postCode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.line4 = line4;
        this.line5 = line5;
        this.postCode = postCode;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getLine3() {
        return line3;
    }

    public String getLine4() {
        return line4;
    }

    public String getLine5() {
        return line5;
    }

    public String getPostCode() {
        return postCode;
    }

    @Override
    public String toString() {
        return "StatDecAppointmentLetterDefendantAddress{" +
                "line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", line3='" + line3 + '\'' +
                ", line4='" + line4 + '\'' +
                ", line5='" + line5 + '\'' +
                ", postCode='" + postCode + '\'' +
                '}';
    }

    public static StatDecAppointmentLetterDefendantAddress.Builder builder() {
        return new StatDecAppointmentLetterDefendantAddress.Builder();
    }

    public static class Builder {

        private String line1;

        private String line2;

        private String line3;

        private String line4;

        private String line5;

        private String postCode;

        public StatDecAppointmentLetterDefendantAddress.Builder withLine1(final String line1) {
            this.line1 = line1;
            return this;
        }

        public StatDecAppointmentLetterDefendantAddress.Builder withLine2(final String line2) {
            this.line2 = line2;
            return this;
        }

        public StatDecAppointmentLetterDefendantAddress.Builder withLine3(final String line3) {
            this.line3 = line3;
            return this;
        }

        public StatDecAppointmentLetterDefendantAddress.Builder withLine4(final String line4) {
            this.line4 = line4;
            return this;
        }

        public StatDecAppointmentLetterDefendantAddress.Builder withLine5(final String line5) {
            this.line5 = line5;
            return this;
        }

        public StatDecAppointmentLetterDefendantAddress.Builder withPostCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        public StatDecAppointmentLetterDefendantAddress build() {
            return new StatDecAppointmentLetterDefendantAddress(line1, line2, line3, line4, line5, postCode);
        }
    }


}
