package uk.gov.moj.cpp.data.anonymization;

import org.junit.Test;
import uk.gov.moj.cpp.data.anonymization.generator.PastDateGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PastDateGeneratorTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void shouldGeneratePastDate() {
        final PastDateGenerator pastDateGenerator = new PastDateGenerator();
        final String pastDate = pastDateGenerator.convert();
        LocalDate localDate = LocalDate.parse(pastDate, FORMATTER);
        assertThat(true, is(localDate.isBefore(LocalDate.now())));
    }
}
