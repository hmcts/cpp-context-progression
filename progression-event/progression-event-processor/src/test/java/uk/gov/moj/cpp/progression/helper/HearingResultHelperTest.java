package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithJudicialResults;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildDefendant;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildHearingWithCourtApplications;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJudicialResult;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildOffence;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Hearing;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultHelperTest {

    @Test
    public void shouldReturnTrueWhenNextHearingExistForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(randomUUID(),
                        asList(buildDefendant(randomUUID(),
                                asList(buildOffence(randomUUID(),
                                        asList(buildJudicialResult(buildNextHearing(randomUUID()))))))))));
        final boolean doesNextHearingExist = new HearingResultHelper().doProsecutionCasesContainNextHearingResults(hearing.getProsecutionCases());
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(true));
    }

    @Test
    public void shouldReturnFalseWhenNoNextHearingExistForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(randomUUID(),
                        asList(buildDefendant(randomUUID(),
                                asList(buildOffence(randomUUID(),
                                        asList(buildJudicialResult(null)))))))));

        final boolean doesNextHearingExist = new HearingResultHelper().doProsecutionCasesContainNextHearingResults(hearing.getProsecutionCases());
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoJudicialResultsExistForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(randomUUID(),
                        asList(buildDefendant(randomUUID(),
                                asList(buildOffence(randomUUID(),null)))))));

        final boolean doesNextHearingExist = new HearingResultHelper().doProsecutionCasesContainNextHearingResults(hearing.getProsecutionCases());
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnTrueWhenNextHearingExistForCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResults(randomUUID(),
                        asList(buildJudicialResult(buildNextHearing(randomUUID()))))));
        final boolean doesNextHearingExist = new HearingResultHelper().doCourtApplicationsContainNextHearingResults(hearing.getCourtApplications());
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(true));
    }

    @Test
    public void shouldReturnFalseWhenNoNextHearingExistForCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResults(randomUUID(),
                        asList(buildJudicialResult(null)))));
        final boolean doesNextHearingExist = new HearingResultHelper().doCourtApplicationsContainNextHearingResults(hearing.getCourtApplications());
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoJudicialResultsExistForCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResults(randomUUID(),null)));
        final boolean doesNextHearingExist = new HearingResultHelper().doCourtApplicationsContainNextHearingResults(hearing.getCourtApplications());
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }
}