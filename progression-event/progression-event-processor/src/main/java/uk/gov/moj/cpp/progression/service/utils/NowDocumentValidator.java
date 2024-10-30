package uk.gov.moj.cpp.progression.service.utils;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;

public class NowDocumentValidator {

    private static final String NO_FIXED_ABODE = "No fixed abode";

    public boolean isPostable(final OrderAddressee orderAddressee) {
        if (isNull(orderAddressee) || isNull(orderAddressee.getAddress())) {
            return false;
        }

        final Nowaddress address = orderAddressee.getAddress();
        return isNotBlank(address.getLine1()) && !NO_FIXED_ABODE.equalsIgnoreCase(address.getLine1()) && isNotBlank(address.getPostCode());
    }
}
