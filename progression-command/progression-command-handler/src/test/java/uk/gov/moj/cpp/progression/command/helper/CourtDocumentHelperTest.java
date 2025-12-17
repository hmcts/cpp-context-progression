package uk.gov.moj.cpp.progression.command.helper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.CourtDocumentHelper.setDefaults;

import uk.gov.justice.core.courts.CourtDocument;

import org.junit.jupiter.api.Test;

public class CourtDocumentHelperTest {

    @Test
    public void shouldSetDefaultToFalse_ForContainsFinancialMeans() {
        final CourtDocument courtDocument = setDefaults(new CourtDocument
                .Builder()
                .build());
        assertThat(courtDocument.getContainsFinancialMeans(), notNullValue());
        assertThat(courtDocument.getContainsFinancialMeans(), is(false));
    }

    @Test
    public void shouldAllowTrue_ForContainsFinancialMeans() {
        assertContainsFinancialMeansSetTo(true);
    }

    @Test
    public void shouldAllowFalse_ForContainsFinancialMeans() {
        assertContainsFinancialMeansSetTo(false);
    }

    private void assertContainsFinancialMeansSetTo(final boolean value) {
        final CourtDocument courtDocument = setDefaults(new CourtDocument
                .Builder()
                .withContainsFinancialMeans(value)
                .build());
        assertThat(courtDocument.getContainsFinancialMeans(), notNullValue());
        assertThat(courtDocument.getContainsFinancialMeans(), is(value));
    }
}