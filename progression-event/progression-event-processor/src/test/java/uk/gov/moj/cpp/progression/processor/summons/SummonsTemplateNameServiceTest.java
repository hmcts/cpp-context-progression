package uk.gov.moj.cpp.progression.processor.summons;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.SummonsRequired.APPLICATION;
import static uk.gov.justice.core.courts.SummonsRequired.BREACH;
import static uk.gov.justice.core.courts.SummonsRequired.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsRequired.SJP_REFERRAL;

import uk.gov.justice.core.courts.SummonsRequired;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class SummonsTemplateNameServiceTest {

    @DataProvider
    public static Object[][] defendantCaseSummonsSpecifications() {
        return new Object[][]{
                {FIRST_HEARING, SummonsCode.MCA, "MCA", "E", false},
                {FIRST_HEARING, SummonsCode.WITNESS, "MCA", "E", false},
                {FIRST_HEARING, SummonsCode.EITHER_WAY, "EitherWay", "E", false},
                {FIRST_HEARING, SummonsCode.MCA, "MCA", "B", true},
                {FIRST_HEARING, SummonsCode.WITNESS, "MCA", "B", true},
                {FIRST_HEARING, SummonsCode.EITHER_WAY, "EitherWay", "B", true},
                {SJP_REFERRAL, null, "SjpReferral", "E", false},
                {SJP_REFERRAL, null, "SjpReferral", "B", true}
        };
    }

    @DataProvider
    public static Object[][] applicationSummonsSpecifications() {
        return new Object[][]{
                {APPLICATION, "Application", "E", false},
                {APPLICATION, "Application", "B", true},
                {BREACH, "Breach", "E", false},
                {BREACH, "Breach", "B", true},
        };
    }

    private SummonsTemplateNameService service = new SummonsTemplateNameService();

    @UseDataProvider("defendantCaseSummonsSpecifications")
    @Test
    public void getCaseSummonsDefendantTemplateName(final SummonsRequired summonsRequired, final SummonsCode summonsCode, final String templateName, final String language, final boolean isWelsh) {
        assertThat(service.getCaseSummonsTemplateName(summonsRequired, summonsCode, isWelsh), is("SP" + language + "_" + templateName));
    }

    @Test
    public void getEnglishFirstHearingCaseSummonsParentTemplateName() {
        assertThat(service.getCaseSummonsParentTemplateName(false), is("SPE_FirstHearingParentGuardian"));
    }

    @Test
    public void getBilingualFirstHearingCaseSummonsParentTemplateName() {
        assertThat(service.getCaseSummonsParentTemplateName(true), is("SPB_FirstHearingParentGuardian"));
    }

    @UseDataProvider("applicationSummonsSpecifications")
    @Test
    public void getApplicationTemplateName(final SummonsRequired summonsRequired, final String templateName, final String language, final boolean isWelsh) {
        assertThat(service.getApplicationTemplateName(summonsRequired, isWelsh), is("SP" + language + "_" + templateName));
    }

    @Test
    public void getEnglishBreachSummonsParentTemplateName() {
        assertThat(service.getBreachSummonsParentTemplateName(false), is("SPE_BreachParent"));
    }

    @Test
    public void getBilingualBreachSummonsParentTemplateName() {
        assertThat(service.getBreachSummonsParentTemplateName(true), is("SPB_BreachParent"));
    }



}