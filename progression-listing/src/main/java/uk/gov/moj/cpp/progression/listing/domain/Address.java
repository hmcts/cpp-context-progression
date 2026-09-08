package uk.gov.moj.cpp.progression.listing.domain;


import static java.util.Optional.empty;

import java.io.Serializable;
import java.util.Optional;

@SuppressWarnings({"squid:S1067","squid:S2065"})
public class Address  implements Serializable {
    private final String address1;

    private transient Optional<String> address2;

    private transient Optional<String> address3;

    private transient Optional<String> address4;

    private transient Optional<String> address5;

    private transient Optional<String> postcode;

    private transient Optional<String> welshAddress1;

    private transient Optional<String> welshAddress2;

    private transient Optional<String> welshAddress3;

    private transient Optional<String> welshAddress4;

    private transient Optional<String> welshAddress5;

    public Address(final String address1, final Optional<String> address2, final Optional<String> address3, final Optional<String> address4, final Optional<String> address5, final Optional<String> postcode, final Optional<String> welshAddress1, final Optional<String> welshAddress2, final Optional<String> welshAddress3, final Optional<String> welshAddress4, final Optional<String> welshAddress5) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postcode = postcode;
        this.welshAddress1 = welshAddress1;
        this.welshAddress2 = welshAddress2;
        this.welshAddress3 = welshAddress3;
        this.welshAddress4 = welshAddress4;
        this.welshAddress5 = welshAddress5;
    }

    public String getAddress1() {
        return address1;
    }

    public Optional<String> getAddress2() {
        return address2.isPresent() ? address2 : empty();
    }

    public Optional<String> getAddress3() {
        return address3.isPresent() ? address3 : empty();
    }

    public Optional<String> getAddress4() {
        return address4.isPresent() ? address4 : empty();
    }

    public Optional<String> getAddress5() {
        return address5.isPresent() ? address5 : empty();
    }

    public Optional<String> getPostcode() {
        return postcode;
    }

    public Optional<String> getWelshAddress1() {
        return welshAddress1.isPresent() ? welshAddress1 : empty();
    }

    public Optional<String> getWelshAddress2() {
        return welshAddress2.isPresent() ? welshAddress2 : empty();
    }

    public Optional<String> getWelshAddress3() {
        return welshAddress3.isPresent() ? welshAddress3 : empty();
    }

    public Optional<String> getWelshAddress4() {
        return welshAddress4.isPresent()? welshAddress4 : empty();
    }

    public Optional<String> getWelshAddress5() {
        return welshAddress5.isPresent() ? welshAddress5 : empty();
    }

    public static Builder address() {
        return new Address.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Address that = (Address) obj;

        return java.util.Objects.equals(this.address1, that.address1) &&
                java.util.Objects.equals(this.address2, that.address2) &&
                java.util.Objects.equals(this.address3, that.address3) &&
                java.util.Objects.equals(this.address4, that.address4) &&
                java.util.Objects.equals(this.address5, that.address5) &&
                java.util.Objects.equals(this.postcode, that.postcode) &&
                java.util.Objects.equals(this.welshAddress1, that.welshAddress1) &&
                java.util.Objects.equals(this.welshAddress2, that.welshAddress2) &&
                java.util.Objects.equals(this.welshAddress3, that.welshAddress3) &&
                java.util.Objects.equals(this.welshAddress4, that.welshAddress4) &&
                java.util.Objects.equals(this.welshAddress5, that.welshAddress5);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(address1, address2, address3, address4, address5, postcode, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5);
    }

    @Override
    public String toString() {
        return "Address{" +
                "address1='" + address1 + "'," +
                "address2='" + address2 + "'," +
                "address3='" + address3 + "'," +
                "address4='" + address4 + "'," +
                "address5='" + address5 + "'," +
                "postcode='" + postcode + "'," +
                "welshAddress1='" + welshAddress1 + "'," +
                "welshAddress2='" + welshAddress2 + "'," +
                "welshAddress3='" + welshAddress3 + "'," +
                "welshAddress4='" + welshAddress4 + "'," +
                "welshAddress5='" + welshAddress5 + "'" +
                "}";
    }

    public static class Builder {

        private transient String address1;

        private transient Optional<String> address2 = empty();

        private transient Optional<String> address3 = empty();

        private transient Optional<String> address4 = empty();

        private transient Optional<String> address5 = empty();

        private transient Optional<String> postcode = empty();

        private transient Optional<String> welshAddress1 = empty();

        private transient Optional<String> welshAddress2 = empty();

        private transient Optional<String> welshAddress3 = empty();

        private transient Optional<String> welshAddress4 = empty();

        private transient Optional<String> welshAddress5 = empty();

        public Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }

        public Builder withAddress2(final Optional<String> address2) {
            this.address2 = address2;
            return this;
        }

        public Builder withAddress3(final Optional<String> address3) {
            this.address3 = address3;
            return this;
        }

        public Builder withAddress4(final Optional<String> address4) {
            this.address4 = address4;
            return this;
        }

        public Builder withAddress5(final Optional<String> address5) {
            this.address5 = address5;
            return this;
        }

        public Builder withPostcode(final Optional<String> postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder withWelshAddress1(final Optional<String> welshAddress1) {
            this.welshAddress1 = welshAddress1;
            return this;
        }

        public Builder withWelshAddress2(final Optional<String> welshAddress2) {
            this.welshAddress2 = welshAddress2;
            return this;
        }

        public Builder withWelshAddress3(final Optional<String> welshAddress3) {
            this.welshAddress3 = welshAddress3;
            return this;
        }

        public Builder withWelshAddress4(final Optional<String> welshAddress4) {
            this.welshAddress4 = welshAddress4;
            return this;
        }

        public Builder withWelshAddress5(final Optional<String> welshAddress5) {
            this.welshAddress5 = welshAddress5;
            return this;
        }

        public Address build() {
            return new Address(address1, address2, address3, address4, address5, postcode, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5);
        }
    }
}
