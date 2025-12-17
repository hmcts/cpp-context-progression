package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;

public class AddressVerificationHelper {
    public static final String SPACE = " ";

    public static void assertAddressDetails(final JsonObject inputAddress,
                                            final String outputAddress,
                                            final String outputPostCode,
                                            final JsonObject defendantAddress) {
        final String actual = addressLines(inputAddress);

        assertThat(actual, is(outputAddress));
        assertThat(inputAddress.getString("postcode"), is(outputPostCode));
        assertThat(inputAddress.getString("address1"), is(defendantAddress.getString("address1")));
        assertThat(inputAddress.getString("address2"), is(defendantAddress.getString("address2")));
        assertThat(inputAddress.getString("address3"), is(defendantAddress.getString("address3")));
        assertThat(inputAddress.getString("address4"), is(defendantAddress.getString("address4")));
        assertThat(inputAddress.getString("address5"), is(defendantAddress.getString("address5")));
        assertThat(inputAddress.getString("postcode"), is(defendantAddress.getString("postCode")));

    }

    public static String addressLines(final DocumentContext prosectionCase, final String addressPath) {
        return addressLines(prosectionCase.read(addressPath));
    }

    public static String addressLines(final JsonObject address) {

        final String addressLineOne = address.getString("address1");
        final String addressLineTwo = address.getString("address2");
        final String addressLineThree = address.getString("address3");
        final String addressLineFour = address.getString("address4");
        final String addressLineFive = address.getString("address5");

        return new StringBuilder(addressLineOne).append(SPACE)
                .append(addressLineTwo)
                .append(SPACE)
                .append(addressLineThree)
                .append(SPACE)
                .append(addressLineFour)
                .append(SPACE)
                .append(addressLineFive).toString();
    }
}
