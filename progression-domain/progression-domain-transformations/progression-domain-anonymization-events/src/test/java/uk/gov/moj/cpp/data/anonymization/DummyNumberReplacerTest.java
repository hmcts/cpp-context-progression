package uk.gov.moj.cpp.data.anonymization;

import org.junit.Test;
import uk.gov.moj.cpp.data.anonymization.generator.DummyNumberReplacer;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class DummyNumberReplacerTest {

    @Test
    public void shouldDummyNumberReplaced() {
        assertThat(DummyNumberReplacer.replace("DummyNumber9999"), equalTo(BigInteger.valueOf(9999)));
    }
}
