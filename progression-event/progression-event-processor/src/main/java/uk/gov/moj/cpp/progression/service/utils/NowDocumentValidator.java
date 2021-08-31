package uk.gov.moj.cpp.progression.service.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;

public class NowDocumentValidator {

    private static final String NO_FIXED_ABODE = "No fixed abode";

    public boolean isPostable(final OrderAddressee orderAddressee) {
        if (nonNull(orderAddressee)) {
            final Nowaddress address = orderAddressee.getAddress();
            return isNotEmpty(address.getLine1()) && !address.getLine1().equalsIgnoreCase(NO_FIXED_ABODE) && isNotEmpty(address.getPostCode());
        }

        return false;
    }
}
