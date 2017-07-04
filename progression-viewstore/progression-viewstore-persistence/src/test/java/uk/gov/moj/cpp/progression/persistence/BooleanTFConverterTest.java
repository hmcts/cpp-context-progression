package uk.gov.moj.cpp.progression.persistence;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.progression.persistence.entity.BooleanTFConverter;

import org.junit.Before;
import org.junit.Test;

public class BooleanTFConverterTest {

    BooleanTFConverter converter;

    @Before
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
