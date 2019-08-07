package uk.gov.moj.cpp.data.anonymization;

import org.apache.commons.validator.routines.EmailValidator;
import org.junit.Test;
import uk.gov.moj.cpp.data.anonymization.generator.EmailGenerator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class EmailGeneratorTest {

    @Test
    public void shouldGenerateARandomEmail() {
        final EmailGenerator emailGenerator = new EmailGenerator();
        final String jsonEmailValue = emailGenerator.convert("test@test.com");
        assertTrue(EmailValidator.getInstance().isValid(jsonEmailValue));
        assertThat(jsonEmailValue, equalTo("xyz@mail.com"));
    }
}
