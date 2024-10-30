package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.hamcrest.CoreMatchers;

public class AddressVerificationHelper {
    public static final String SPACE = " ";

    public static void assertAddressDetails(final JsonObject inputAddress,
                                            final String outputAddress,
                                            final String outputPostCode,
                                            final JsonObject defendantAddress) {
        final String actual = addressLines(inputAddress);
        assertThat(actual, is(outputAddress));
        assertThat(inputAddress.getString("postcode"), is(outputPostCode));
        assertThat(inputAddress.getString("address1"), CoreMatchers.is(defendantAddress.getString("address1")));
        assertThat(inputAddress.getString("address2"), CoreMatchers.is(defendantAddress.getString("address2")));
        assertThat(inputAddress.getString("address3"), CoreMatchers.is(defendantAddress.getString("address3")));
        assertThat(inputAddress.getString("address4"), CoreMatchers.is(defendantAddress.getString("address4")));
        assertThat(inputAddress.getString("address5"), CoreMatchers.is(defendantAddress.getString("address5")));
        assertThat(inputAddress.getString("postcode"), CoreMatchers.is(defendantAddress.getString("postCode")));

    }

    public static String addressLines(final DocumentContext prosectionCase, final String addressPath) {
        final JsonObject address = prosectionCase.read(addressPath);

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
