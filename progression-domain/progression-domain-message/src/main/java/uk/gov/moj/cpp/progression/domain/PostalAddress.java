package uk.gov.moj.cpp.progression.domain;

public class PostalAddress {

    private String line1;

    private String line2;

    private String line3;

    private String postCode;

    public PostalAddress(final String line1, final String line2, final String line3, final String postCode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
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

    public String getPostCode() {
        return postCode;
    }

    @Override
    public String toString() {
        return "PostalAddress{" +
                "line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", line3='" + line3 + '\'' +
                ", postCode='" + postCode + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String line1;

        private String line2;

        private String line3;

        private String postCode;

        public Builder withLine1(final String line1) {
            this.line1 = line1;
            return this;
        }

        public Builder withLine2(final String line2) {
            this.line2 = line2;
            return this;
        }

        public Builder withLine3(final String line3) {
            this.line3 = line3;
            return this;
        }

        public Builder withPostCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        public PostalAddress build() {
            return new PostalAddress(line1, line2, line3, postCode);
        }
    }
}
