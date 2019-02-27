package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String postCode;

    public Address(final String address1, final String address2, final String address3, final String address4, final String postCode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.postCode = postCode;
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

    public String getPostCode() {
        return postCode;
    }
}
