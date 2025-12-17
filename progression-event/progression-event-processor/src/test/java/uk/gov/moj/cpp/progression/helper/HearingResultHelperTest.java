package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithJudicialResults;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithJudicialResultsUnderCourtApplicationCases;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtApplicationWithJudicialResultsUnderCourtOrders;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildDefendant;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildHearingWithCourtApplications;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJudicialResult;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildNextHearing;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildOffence;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildProsecutionCase;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.moj.cpp.progression.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingResultHelperTest {

    private static final UUID APPROVED_SUMMONS = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");

    @Test
    public void shouldReturnTrueWhenNextHearingExistForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(randomUUID(),
                        asList(buildDefendant(randomUUID(),
                                asList(buildOffence(randomUUID(),
                                        asList(buildJudicialResult(buildNextHearing(randomUUID()))))))))));
        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(true));
    }

    @Test
    public void shouldReturnFalseWhenNoNextHearingExistForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(randomUUID(),
                        asList(buildDefendant(randomUUID(),
                                asList(buildOffence(randomUUID(),
                                        asList(buildJudicialResult(null)))))))));

        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoJudicialResultsExistForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(randomUUID(),
                        asList(buildDefendant(randomUUID(),
                                asList(buildOffence(randomUUID(),null)))))));

        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnTrueWhenNextHearingExistForCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResults(randomUUID(),
                        asList(buildJudicialResult(buildNextHearing(randomUUID()))))));
        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(true));
    }
    @Test
    public void shouldReturnAllJudicialResults(){
        final UUID hearingId = randomUUID();
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(buildJudicialResult(buildNextHearing(hearingId)));
        final CourtApplication courtApplication = buildCourtApplicationWithJudicialResults(randomUUID(), judicialResults);
        final List<JudicialResult> judicialResultList = new HearingResultHelper().getAllJudicialResultsFromApplication(courtApplication);
        MatcherAssert.assertThat(judicialResultList.size(), CoreMatchers.is(2));
    }

    @Test
    public void shouldReturnTrueWhenNextHearingExistForCourtOrdersUnderCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResultsUnderCourtOrders(randomUUID(),
                        asList(buildJudicialResult(buildNextHearing(randomUUID()))))));
        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(true));
    }

    @Test
    public void shouldReturnTrueWhenNextHearingExistForCourtApplicationCasesUnderCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResultsUnderCourtApplicationCases(randomUUID(),
                        asList(buildJudicialResult(buildNextHearing(randomUUID()))))));
        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(true));
    }

    @Test
    public void shouldReturnFalseWhenNoNextHearingExistForCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResults(randomUUID(),
                        asList(buildJudicialResult(null)))));
        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoJudicialResultsExistForCourtApplications() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildCourtApplicationWithJudicialResults(randomUUID(),null)));
        final boolean doesNextHearingExist = new HearingResultHelper().doHearingContainNextHearingResults(hearing);
        MatcherAssert.assertThat(doesNextHearingExist, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoApprovedSummonsExistForCourtApplications() {
        final boolean summonsApproved = new HearingResultHelper().isSummonsApproved(
                buildCourtApplicationWithJudicialResults(randomUUID(),
                        Arrays.asList(JudicialResult.judicialResult().withJudicialResultTypeId(randomUUID()).build())));
        MatcherAssert.assertThat(summonsApproved, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnTrueWhenApprovedSummonsExistForCourtApplications() {
        final boolean summonsApproved = new HearingResultHelper().isSummonsApproved(
                buildCourtApplicationWithJudicialResults(randomUUID(),
                        Arrays.asList(JudicialResult.judicialResult().withJudicialResultTypeId(APPROVED_SUMMONS).build())));
        MatcherAssert.assertThat(summonsApproved, CoreMatchers.is(true));
    }

    @Test
    public void shouldReturnFalseIsSummonsRequiredForRespondents() {
        List<CourtApplicationParty> respondents = Arrays.asList( CourtApplicationParty.courtApplicationParty().withSummonsRequired(false).build(),
                CourtApplicationParty.courtApplicationParty().withSummonsRequired(false).build());
        final boolean summonsApproved = new HearingResultHelper().isSummonsRequiredForRespondents(respondents);
        MatcherAssert.assertThat(summonsApproved, CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnTrueIsSummonsRequiredForRespondents() {
        List<CourtApplicationParty> respondents = Arrays.asList( CourtApplicationParty.courtApplicationParty().withSummonsRequired(true).build(),
                CourtApplicationParty.courtApplicationParty().withSummonsRequired(true).build());
        final boolean summonsApproved = new HearingResultHelper().isSummonsRequiredForRespondents(respondents);
        MatcherAssert.assertThat(summonsApproved, CoreMatchers.is(true));
    }

    @Test
    public void shouldTestExceptionForSummonsRequiredForRespondents() {
        List<CourtApplicationParty> respondents = Arrays.asList( CourtApplicationParty.courtApplicationParty().withSummonsRequired(false).build(),
                CourtApplicationParty.courtApplicationParty().withSummonsRequired(true).build(),
                CourtApplicationParty.courtApplicationParty().withSummonsRequired(false).build());
        assertThrows(DataValidationException.class, () -> new HearingResultHelper().isSummonsRequiredForRespondents(respondents));
    }
}