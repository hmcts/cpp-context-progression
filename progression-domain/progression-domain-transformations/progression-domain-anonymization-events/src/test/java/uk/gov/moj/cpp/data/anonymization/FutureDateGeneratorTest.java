package uk.gov.moj.cpp.data.anonymization;

import org.junit.Test;
import uk.gov.moj.cpp.data.anonymization.generator.FutureDateGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FutureDateGeneratorTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void shouldGenerateFutureDate() {
        final FutureDateGenerator futureDateGenerator = new FutureDateGenerator();
        final String futureDate = futureDateGenerator.convert();
        LocalDate localDate = LocalDate.parse(futureDate, FORMATTER);
        assertThat(true, is(localDate.isAfter(LocalDate.now())));
    }
}
