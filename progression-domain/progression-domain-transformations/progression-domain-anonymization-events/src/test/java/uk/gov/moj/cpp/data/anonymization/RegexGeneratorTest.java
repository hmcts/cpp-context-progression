package uk.gov.moj.cpp.data.anonymization;

import org.apache.commons.validator.routines.RegexValidator;
import org.junit.Test;
import uk.gov.moj.cpp.data.anonymization.generator.RegexGenerator;

import java.util.UUID;
import java.util.regex.Pattern;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RegexGeneratorTest {

    private static final String UUID_PATTERN = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    private final RegexGenerator regexGenerator = new RegexGenerator(Pattern.compile(UUID_PATTERN));

    @Test
    public void shouldGenerateARandomStringFromARegularExpression() {

        final String randomString = regexGenerator.convert("test");

        final RegexValidator regexValidator = new RegexValidator(UUID_PATTERN);

        assertTrue(regexValidator.isValid(randomString));

        final UUID uuid = fromString(randomString);

        assertThat(uuid, is(notNullValue()));

        assertThat(regexGenerator.convert("test"), is(not(equalTo(randomString))));

    }

}