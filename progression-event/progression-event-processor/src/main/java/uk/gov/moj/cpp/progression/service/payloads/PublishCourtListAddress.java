package uk.gov.moj.cpp.progression.service.payloads;

public class PublishCourtListAddress {
    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String postCode;
    private final boolean isWelsh;

    private PublishCourtListAddress(final String line1, final String line2, final String line3, final String line4, final String line5, final String postCode, final boolean isWelsh) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.line4 = line4;
        this.line5 = line5;
        this.postCode = postCode;
        this.isWelsh = isWelsh;
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

    public boolean getIsWelsh() {
        return isWelsh;
    }

    public static PublishCourtListAddressBuilder publishCourtListAddressBuilder() {
        return new PublishCourtListAddressBuilder();
    }

    public static final class PublishCourtListAddressBuilder {
        private String line1;
        private String line2;
        private String line3;
        private String line4;
        private String line5;
        private String postCode;
        private boolean isWelsh;

        private PublishCourtListAddressBuilder() {
        }

        public PublishCourtListAddressBuilder withLine1(final String line1) {
            this.line1 = line1;
            return this;
        }

        public PublishCourtListAddressBuilder withLine2(final String line2) {
            this.line2 = line2;
            return this;
        }

        public PublishCourtListAddressBuilder withLine3(final String line3) {
            this.line3 = line3;
            return this;
        }

        public PublishCourtListAddressBuilder withLine4(final String line4) {
            this.line4 = line4;
            return this;
        }

        public PublishCourtListAddressBuilder withLine5(final String line5) {
            this.line5 = line5;
            return this;
        }

        public PublishCourtListAddressBuilder withPostCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        public PublishCourtListAddressBuilder withIsWelsh(final boolean isWelsh) {
            this.isWelsh = isWelsh;
            return this;
        }

        public PublishCourtListAddress build() {
            return new PublishCourtListAddress(line1, line2, line3, line4, line5, postCode, isWelsh);
        }
    }
}
