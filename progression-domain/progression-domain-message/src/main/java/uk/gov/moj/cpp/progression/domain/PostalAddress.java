package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;

@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize", "squid:S00107"})
public class PostalAddress implements Serializable {

    private static final long serialVersionUID = -6377151391595272633L;

    private String line1;

    private String line2;

    private String line3;

    private String line4;

    private String line5;

    private String postCode;

    public PostalAddress(final String line1, final String line2, final String line3,String line4,String line5, final String postCode) {
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
        return "PostalAddress{" +
                "line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", line3='" + line3 + '\'' +
                ", line4='" + line4 + '\'' +
                ", line5='" + line5 + '\'' +
                ", postCode='" + postCode + '\'' +
                '}';
    }
    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String line1;

        private String line2;

        private String line3;

        private String line4;

        private String line5;

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

        public Builder withLine4(final String line4) {
            this.line4 = line4;
            return this;
        }
        public Builder withLine5(final String line5) {
            this.line5 = line5;
            return this;
        }
        public Builder withPostCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        public PostalAddress build() {
            return new PostalAddress(line1, line2, line3,line4,line5, postCode);
        }
    }
}
