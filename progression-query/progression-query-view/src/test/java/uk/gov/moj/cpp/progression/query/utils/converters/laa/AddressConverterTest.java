package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import uk.gov.justice.progression.query.laa.Address;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressConverterTest {

    @InjectMocks
    private AddressConverter addressConverter;

    @Test
    void testConvertAddress() {
        final uk.gov.justice.core.courts.Address address = uk.gov.justice.core.courts.Address.address()
                .withAddress1("123 Main St")
                .withAddress2("Apt 4B")
                .withAddress3("Springfield")
                .withAddress4("IL")
                .withPostcode("62704")
                .build();

        Address result = addressConverter.convert(address);

        assertThat(result, notNullValue());
        assertThat(result.getAddress1(), is(address.getAddress1()));
        assertThat(result.getAddress2(), is(address.getAddress2()));
        assertThat(result.getAddress3(), is(address.getAddress3()));
        assertThat(result.getAddress4(), is(address.getAddress4()));
        assertThat(result.getPostCode(), is(address.getPostcode()));
    }

    @Test
    void testConvertAddress_NullInput() {
        final Address result = addressConverter.convert(null);
        assertThat(result, nullValue());
    }
}