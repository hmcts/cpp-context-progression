package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Address implements Serializable{
    private static final long serialVersionUID = -8915924497980960162L;
    private String address1;
    private String address2;
    private String address3;
    private String address4;
    private String postcode;

    public String getAddress1() {
        return this.address1;
    }

    public void setAddress1(final String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return this.address2;
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

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(final String postcode) {
        this.postcode = postcode;
    }
}
