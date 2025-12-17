package uk.gov.moj.cpp.progression.service.payloads;

public class PublishCourtListCourtAddress {
    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String postCode;
    private final boolean isWelsh;
    private final String line1Welsh;
    private final String line2Welsh;
    private final String line3Welsh;
    private final String line4Welsh;
    private final String line5Welsh;

    public PublishCourtListCourtAddress(final String line1, final String line2, final String line3, final String line4, final String line5,
                                        final String postCode, final boolean isWelsh, final String line1Welsh, final String line2Welsh,
                                        final String line3Welsh, final String line4Welsh, final String line5Welsh) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.line4 = line4;
        this.line5 = line5;
        this.postCode = postCode;
        this.isWelsh = isWelsh;
        this.line1Welsh = line1Welsh;
        this.line2Welsh = line2Welsh;
        this.line3Welsh = line3Welsh;
        this.line4Welsh = line4Welsh;
        this.line5Welsh = line5Welsh;
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

    public String getLine1Welsh() {
        return line1Welsh;
    }

    public String getLine2Welsh() {
        return line2Welsh;
    }

    public String getLine3Welsh() {
        return line3Welsh;
    }

    public String getLine4Welsh() {
        return line4Welsh;
    }

    public String getLine5Welsh() {
        return line5Welsh;
    }

    public static PublishCourtListCourtAddressBuilder publishCourtListCourtAddressBuilder() {
        return new PublishCourtListCourtAddressBuilder();
    }

    public static final class PublishCourtListCourtAddressBuilder {
        private String line1;
        private String line2;
        private String line3;
        private String line4;
        private String line5;
        private String postCode;
        private boolean isWelsh;
        private String line1Welsh;
        private String line2Welsh;
        private String line3Welsh;
        private String line4Welsh;
        private String line5Welsh;

        private PublishCourtListCourtAddressBuilder() {
        }

        public PublishCourtListCourtAddressBuilder withLine1(final String line1) {
            this.line1 = line1;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine2(final String line2) {
            this.line2 = line2;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine3(final String line3) {
            this.line3 = line3;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine4(final String line4) {
            this.line4 = line4;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine5(final String line5) {
            this.line5 = line5;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withPostCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withIsWelsh(final boolean isWelsh) {
            this.isWelsh = isWelsh;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine1Welsh(final String line1Welsh) {
            this.line1Welsh = line1Welsh;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine2Welsh(final String line2Welsh) {
            this.line2Welsh = line2Welsh;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine3Welsh(final String line3Welsh) {
            this.line3Welsh = line3Welsh;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine4Welsh(final String line4Welsh) {
            this.line4Welsh = line4Welsh;
            return this;
        }

        public PublishCourtListCourtAddressBuilder withLine5Welsh(final String line5Welsh) {
            this.line5Welsh = line5Welsh;
            return this;
        }

        public PublishCourtListCourtAddress build() {
            return new PublishCourtListCourtAddress(line1, line2, line3, line4, line5, postCode, isWelsh, line1Welsh, line2Welsh, line3Welsh, line4Welsh, line5Welsh);
        }
    }
}
