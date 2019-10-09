package uk.gov.moj.cpp.data.anonymization;

import org.junit.Test;
import uk.gov.moj.cpp.data.anonymization.generator.SimpleStringGenerator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StringValueGeneratorTest {

    @Test
    public void shouldGenerateARandomString() {
        final SimpleStringGenerator stringValueGenerator = new SimpleStringGenerator();
        assertThat(stringValueGenerator.convert(), equalTo("XXXXX"));
    }
}
