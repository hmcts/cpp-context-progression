package uk.gov.moj.cpp.progression.formatters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class AccountingDivisionCodeFormatterTest {

    @Test
    public void shouldFormatSingleDigitAccountDivisionCode() throws Exception {
        assertThat(AccountingDivisionCodeFormatter.formatAccountingDivisionCode("7"), is("007"));
        assertThat(AccountingDivisionCodeFormatter.formatAccountingDivisionCode("0"), is("000"));
    }

    @Test
    public void shouldFormatDoubleDigitAccountDivisionCode() throws Exception {
        assertThat(AccountingDivisionCodeFormatter.formatAccountingDivisionCode("87"), is("087"));
        assertThat(AccountingDivisionCodeFormatter.formatAccountingDivisionCode("07"), is("007"));
    }

    @Test
    public void shouldFormatThreeDigitAccountDivisionCode() throws Exception {
        assertThat(AccountingDivisionCodeFormatter.formatAccountingDivisionCode("187"), is("187"));
        assertThat(AccountingDivisionCodeFormatter.formatAccountingDivisionCode("000"), is("000"));
    }
}
