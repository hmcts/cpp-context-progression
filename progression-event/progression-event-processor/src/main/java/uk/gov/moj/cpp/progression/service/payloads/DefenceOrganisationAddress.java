package uk.gov.moj.cpp.progression.service.payloads;

public class DefenceOrganisationAddress {
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String addressPostcode;

    public DefenceOrganisationAddress(final String address1, final String address2, final String address3, final String address4, final String addressPostcode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.addressPostcode = addressPostcode;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public static Builder defenceOrganisationAddressBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String addressPostcode;

        private Builder() {
        }

        public Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }

        public Builder withAddress2(final String address2) {
            this.address2 = address2;
            return this;
        }

        public Builder withAddress3(final String address3) {
            this.address3 = address3;
            return this;
        }

        public Builder withAddress4(final String address4) {
            this.address4 = address4;
            return this;
        }

        public Builder withAddressPostcode(final String addressPostcode) {
            this.addressPostcode = addressPostcode;
            return this;
        }

        public DefenceOrganisationAddress build() {
            return new DefenceOrganisationAddress(address1, address2, address3, address4, addressPostcode);
        }
    }
}
