package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import uk.gov.justice.progression.query.laa.Address;

import java.util.Objects;

@SuppressWarnings("squid:S1168")
public class AddressConverter extends LAAConverter {

    public Address convert(final uk.gov.justice.core.courts.Address address) {
        if (Objects.isNull(address)) {
            return null;
        }
        return Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withPostCode(address.getPostcode())
                .build();
    }
}