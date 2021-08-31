package uk.gov.moj.cpp.progression.service.utils;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.core.courts.nowdocument.Nowaddress.nowaddress;
import static uk.gov.justice.core.courts.nowdocument.OrderAddressee.orderAddressee;

import uk.gov.justice.core.courts.nowdocument.OrderAddressee;

import org.junit.Test;

public class NowDocumentValidatorTest {

    private NowDocumentValidator nowDocumentValidator = new NowDocumentValidator();
    private static final String NO_FIXED_ABODE = "No fixed abode";

    @Test
    public void shouldSendIsPostableAsTrue() {
        final boolean postable = nowDocumentValidator.isPostable(getOrderAddressee("High Street Croydon", "CR06CF"));

        assertThat(postable, is(TRUE));
    }

    @Test
    public void shouldSendIsPostableAsFalse() {
        final boolean postable = nowDocumentValidator.isPostable(getOrderAddressee(NO_FIXED_ABODE, null));

        assertThat(postable, is(FALSE));
    }

    @Test
    public void shouldSendIsPostableAsFalseWhenNoPostcodePresent() {
        final boolean postable = nowDocumentValidator.isPostable(getOrderAddressee("High Street Croydon", null));
        assertThat(postable, is(FALSE));
    }

    @Test
    public void shouldSendIsPostableAsFalseWhenOrderAddresseeIsNull() {
        final boolean postable = nowDocumentValidator.isPostable(null);
        assertThat(postable, is(FALSE));
    }

    private OrderAddressee getOrderAddressee(final String address, final String postCode) {
        return orderAddressee().withAddress(nowaddress().withLine1(address).withPostCode(postCode).build()).build();
    }
}