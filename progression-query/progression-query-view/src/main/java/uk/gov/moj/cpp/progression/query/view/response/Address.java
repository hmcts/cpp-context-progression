package uk.gov.moj.cpp.progression.query.view.response;

import java.util.UUID;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class Address {

    private UUID addressId;

    private String address1;

    private String address2;

    private String address3;

    private String address4;

    private String postCode;

    public UUID getAddressId() {
        return addressId;
    }

    public void setAddressId(final UUID addressId) {
        this.addressId = addressId;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(final String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(final String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(final String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(final String address4) {
        this.address4 = address4;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(final String postCode) {
        this.postCode = postCode;
    }

    public Builder builder() {
        return new Builder();
    }

    class Builder {

        public Builder addressId(final UUID addressId) {
            setAddressId(addressId);
            return this;
        }

        public Builder address1(final String address1) {
            setAddress1(address1);
            return this;
        }

        public Builder address2(final String address2) {
            setAddress2(address2);
            return this;
        }

        public Builder address3(final String address3) {
            setAddress3(address3);
            return this;
        }

        public Builder address4(final String address4) {
            setAddress4(address4);
            return this;
        }

        public Builder postCode(final String postCode) {
            setPostCode(postCode);
            return this;
        }

        public Address build() {
            final Address address = new Address();
            address.setAddress1(address1);
            address.setAddress2(address2);
            address.setAddress3(address3);
            address.setAddress4(address4);
            address.setAddressId(addressId);
            return address;
        }

    }

}
