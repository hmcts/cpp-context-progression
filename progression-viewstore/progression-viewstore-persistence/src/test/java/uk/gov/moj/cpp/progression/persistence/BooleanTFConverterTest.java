package uk.gov.moj.cpp.progression.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.moj.cpp.progression.persistence.entity.BooleanTFConverter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @deprecated This is deprecated for Release 2.4
 */
@Deprecated
public class BooleanTFConverterTest {

    BooleanTFConverter converter;

    @BeforeEach
    public void SetUp() {
        converter = new BooleanTFConverter();
    }

    @Test
    public void convertToDatabaseColumnTest() throws Exception {
        assertNull(converter.convertToDatabaseColumn(null));
        assertThat(converter.convertToDatabaseColumn(true), equalTo("T"));
        assertThat(converter.convertToDatabaseColumn(false), equalTo("F"));
    }

    @Test
    public void convertToEntityAttributeTest() throws Exception {
        assertNull(converter.convertToEntityAttribute(null));
        assertThat(converter.convertToEntityAttribute("T"), equalTo(true));
        assertThat(converter.convertToEntityAttribute("F"), equalTo(false));
    }

}
