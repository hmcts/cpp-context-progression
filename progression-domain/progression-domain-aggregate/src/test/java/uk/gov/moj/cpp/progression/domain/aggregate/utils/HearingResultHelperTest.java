package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.Verdict;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultHelperTest {

    private static final UUID HEARING_ID_1 = fromString("dee1acd3-5c18-4417-9397-36c5257ac6b2");
    private static final UUID HEARING_ID_2 = fromString("09d5075a-c780-490d-976c-ef803487db2c");

    private static final UUID PROSECUTION_CASE_ID_1 = fromString("993ae52d-9922-4209-8312-60b42e6d662b");
    private static final UUID PROSECUTION_CASE_ID_2 = fromString("599be8ac-9def-44ff-a5d9-5a33830c1d03");

    private static final UUID DEFENDANT_ID_1 = fromString("11fedc85-ed52-49e4-b441-83ba91b6fd2a");
    private static final UUID DEFENDANT_ID_2 = fromString("d5921a8c-9acd-4f57-b62a-7ac6a4ea0f62");

    private static final UUID OFFENCE_ID_1 = fromString("a98e6235-9d4d-47f0-9c09-81a76dba3caf");
    private static final UUID OFFENCE_ID_2 = fromString("8e7b4098-2d4c-4ecf-b2d0-499bdb5889c2");

    private final static String COMMITTING_COURT_CODE = "CCCODE";
    private final static String COMMITTING_COURT_NAME = "Committing Court";

    private static final UUID REPORTING_RESTRICTION_ID_1 = fromString("6794cc13-e490-41a0-ba95-bf18590e37e6");

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForOneOffenceForOneDefendantInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant, DEFENDANT_ID_1, 1);

        final Offence offence = defendant.getOffences().get(0);
        assertOffence(offence, OFFENCE_ID_1);

        final ReportingRestriction reportingRestriction = offence.getReportingRestrictions().get(0);
        assertReportingRestriction(reportingRestriction, REPORTING_RESTRICTION_ID_1);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForTwoOffencesForOneDefendantInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(
                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))),
                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                )
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant, DEFENDANT_ID_1, 2);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForTwoOffencesForTheTwoDefendantsInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(
                                buildDefendant(DEFENDANT_ID_1,
                                        of(
                                                buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                        ).collect(Collectors.toList())),
                                buildDefendant(DEFENDANT_ID_2,
                                        of(
                                                buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                        ).collect(Collectors.toList()))
                        ).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 2);

        final Defendant defendant1 = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final Defendant defendant2 = prosecutionCase.getDefendants().get(1);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForTwoOffencesForTheTwoDefendantsInTwoProsecutionCase() {
        final Hearing hearing = buildHearing(
                of(
                        buildProsecutionCase(PROSECUTION_CASE_ID_1,
                                of(
                                        buildDefendant(DEFENDANT_ID_1,
                                                of(
                                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList())),
                        buildProsecutionCase(PROSECUTION_CASE_ID_2,
                                of(
                                        buildDefendant(DEFENDANT_ID_2,
                                                of(
                                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList()))
                ).collect(Collectors.toList())
        );

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 2);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_2, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds.getProsecutionCases().get(1);
        assertProsecutionCase(prosecutionCase2, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldReturnTwoHearingsWhenTwoNextHearingAvailableForTwoOffencesForOneDefendantInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(
                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))),
                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_2))))
                                )
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(2));

        final HearingListingNeeds hearingListingNeeds1 = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds1, HEARING_ID_2, 1);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds1.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_1, 1);

        final HearingListingNeeds hearingListingNeeds2 = nextHearingDetails.getHearingListingNeedsList().get(1);
        assertHearing(hearingListingNeeds2, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds2.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldReturnTwoHearingsWhenTwoNextHearingsAvailableForTwoOffencesForTheTwoDefendantsInOneProsecutionCase() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(
                                buildDefendant(DEFENDANT_ID_1,
                                        of(
                                                buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                        ).collect(Collectors.toList())),
                                buildDefendant(DEFENDANT_ID_2,
                                        of(
                                                buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_2))))
                                        ).collect(Collectors.toList()))
                        ).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(2));

        final HearingListingNeeds hearingListingNeeds1 = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds1, HEARING_ID_2, 1);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds1.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final HearingListingNeeds hearingListingNeeds2 = nextHearingDetails.getHearingListingNeedsList().get(1);
        assertHearing(hearingListingNeeds2, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds2.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase2, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);

    }

    @Test
    public void shouldReturnTwoHearingsWhenTwoNextHearingsAvailableForTwoOffencesForTheTwoDefendantsInTwoProsecutionCase() {
        final Hearing hearing = buildHearing(
                of(
                        buildProsecutionCase(PROSECUTION_CASE_ID_1,
                                of(
                                        buildDefendant(DEFENDANT_ID_1,
                                                of(
                                                        buildOffence(OFFENCE_ID_1, asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList())),
                        buildProsecutionCase(PROSECUTION_CASE_ID_2,
                                of(
                                        buildDefendant(DEFENDANT_ID_2,
                                                of(
                                                        buildOffence(OFFENCE_ID_2, asList(buildJudicialResult(buildNextHearing(HEARING_ID_2))))
                                                ).collect(Collectors.toList()))
                                ).collect(Collectors.toList()))
                ).collect(Collectors.toList())
        );

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(2));

        final HearingListingNeeds hearingListingNeeds1 = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds1, HEARING_ID_2, 1);

        final ProsecutionCase prosecutionCase1 = hearingListingNeeds1.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase1, PROSECUTION_CASE_ID_2, 1);

        final Defendant defendant1 = prosecutionCase1.getDefendants().get(0);
        assertDefendant(defendant1, DEFENDANT_ID_2, 1);

        final HearingListingNeeds hearingListingNeeds2 = nextHearingDetails.getHearingListingNeedsList().get(1);
        assertHearing(hearingListingNeeds2, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase2 = hearingListingNeeds2.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase2, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant2 = prosecutionCase2.getDefendants().get(0);
        assertDefendant(defendant2, DEFENDANT_ID_1, 1);
    }

    @Test
    public void shouldNotReturnHearingWhenNoNextHearingAvailableForOffences() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1, null))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(0));
    }

    @Test
    public void shouldNotReturnHearingWhenOnlyDefendantJudicialResultsAreAvailable() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1, null))
                                        .collect(Collectors.toList()), asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))
                        )).collect(Collectors.toList()))));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(0));
    }

    @Test
    public void shouldReturnFalseForUnscheduledNextHearingsRequiredForProsecutionCases() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1, null))
                                        .collect(Collectors.toList()), asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))
                        )).collect(Collectors.toList()))));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);

        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(false));
    }

    @Test
    public void shouldReturnTrueForProsecutionCasesWithUnscheduledResults() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildUnscheduledJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplications(PROSECUTION_CASE_ID_1)));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);

        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(true));
    }

    @Test
    public void shouldReturnTrueForProsecutionCasesWithUnscheduledNextHearingDate() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearingWithNoFixedNextHearingDate(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplications(PROSECUTION_CASE_ID_1)));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);
        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(true));
    }


    @Test
    public void shouldReturnFalseForUnscheduledNextHearingsRequiredForCourtApplications() {
        final Hearing hearing = buildHearing(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1, null))
                                        .collect(Collectors.toList()), asList(buildJudicialResult(buildNextHearing(HEARING_ID_1)))
                        )).collect(Collectors.toList()))));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);

        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(false));
    }

    @Test
    public void shouldReturnTrueForCourtApplicationsWithUnscheduledResults() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplicationsAndUnscheduledJudicialResults(PROSECUTION_CASE_ID_1, true)));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);

        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(true));
    }

    @Test
    public void shouldReturnFalseForCourtApplicationsWithUnscheduledResultsThatHasNotBeenAmended() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplicationsAndUnscheduledJudicialResults(PROSECUTION_CASE_ID_1, false)));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);

        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(false));
    }

    @Test
    public void shouldReturnTrueForCourtApplicationsWithUnscheduledNextHearingDate() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplicationsWithNoFixedDateOfNextHearing(PROSECUTION_CASE_ID_1)));

        final boolean unscheduledNextHearingsRequiredForCourtApplication = HearingResultHelper.unscheduledNextHearingsRequiredFor(hearing);
        assertThat(unscheduledNextHearingsRequiredForCourtApplication, is(true));
    }

    @Test
    public void shouldReturnTrueWhenCourtApplicationsContainRelatedNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                emptyList(),
                asList(buildCourtApplicationsWithRelatedNextHearingJudicialResults(PROSECUTION_CASE_ID_1, true)));

        assertThat(HearingResultHelper.hasHearingContainsRelatedNextHearings(hearing), is(true));
    }

    @Test
    public void shouldReturnFalseWhenCourtApplicationsContainRelatedNextHearingThatHasNotBeenAmended() {
        final Hearing hearing = buildHearingWithCourtApplications(
                emptyList(),
                asList(buildCourtApplicationsWithRelatedNextHearingJudicialResults(PROSECUTION_CASE_ID_1, false)));

        assertThat(HearingResultHelper.hasHearingContainsRelatedNextHearings(hearing), is(false));
    }

    @Test
    public void shouldReturnTrueForProsecutionCasesContainRelatedNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        assertThat(HearingResultHelper.hasHearingContainsRelatedNextHearings(hearing), is(true));
    }

    @Test
    public void shouldReturnFalseWhenProsecutionCasesOrCourtApplicationsDoNotContainRelatedNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing())),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplicationsWithoutRelatedNextHearing(PROSECUTION_CASE_ID_1)));

        assertThat(HearingResultHelper.hasHearingContainsRelatedNextHearings(hearing), is(false));
    }

    @Test
    public void shouldReturnTrueWhenCourtApplicationsContainNewNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                emptyList(),
                asList(buildCourtApplicationsWithNewNextHearingJudicialResults(randomUUID())));

        assertThat(HearingResultHelper.hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing(hearing), is(true));
    }

    @Test
    public void shouldReturnTrueForProsecutionCasesOutsideMultiDayHearingContainNewNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing())),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        assertThat(HearingResultHelper.hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing(hearing), is(true));
    }


    @Test
    public void shouldReturnFalseWhenProsecutionCasesOrCourtApplicationsOutsideMultiDayHearingDoNotContainNewNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing(randomUUID()))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(buildCourtApplications(PROSECUTION_CASE_ID_1)));

        assertThat(HearingResultHelper.hasNewNextHearingsAndNextHearingOutsideOfMultiDaysHearing(hearing), is(false));
    }

    @Test
    public void shouldReturnTrueForProsecutionCasesContainNewNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResult(buildNextHearing())),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        assertThat(HearingResultHelper.doHearingContainNewOrAmendedNextHearingResults(hearing), is(true));
    }

    @Test
    public void shouldReturnFalseForProsecutionCasesContainNewNextHearingWhenNextHearingResultsNotAmended() {
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResultWithAmendmentAs(buildNextHearing(), false)),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        assertThat(HearingResultHelper.doHearingContainNewOrAmendedNextHearingResults(hearing), is(false));
    }

    @Test
    public void shouldReturnTrueForApplicationContainsNewNextHearing() {
        final Hearing hearing = buildHearingWithCourtApplications(
                emptyList(),
                asList(buildCourtApplicationsWithRelatedNextHearingJudicialResults(PROSECUTION_CASE_ID_1, true)));

        assertThat(HearingResultHelper.doHearingContainNewOrAmendedNextHearingResults(hearing), is(true));
    }

    @Test
    public void shouldReturnFalseForApplicationContainsNewNextHearingWhenNextHearingResultsNotAmended() {
        final Hearing hearing = buildHearingWithCourtApplications(
                emptyList(),
                asList(buildCourtApplicationsWithRelatedNextHearingJudicialResults(PROSECUTION_CASE_ID_1, false)));

        assertThat(HearingResultHelper.doHearingContainNewOrAmendedNextHearingResults(hearing), is(false));
    }

    @Test
    public void shouldReturnFalseWhenFirstTimeResult(){
        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResultWithAmendmentAs(buildNextHearing(), false)),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        assertThat(HearingResultHelper.isNextHearingDeleted(hearing, null), is(false));
    }

    @Test
    public void shouldReturnTrueWhenNextHearingDeletedInResultForCase(){
        final Hearing oldHearingResult = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResultWithAmendmentAs(buildNextHearing(), false)),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        final Hearing newHearingResult = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildJudicialResultWithAmendmentAs(null, false)),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                emptyList());

        assertThat(HearingResultHelper.isNextHearingDeleted(newHearingResult, oldHearingResult), is(true));
    }

    @Test
    public void shouldReturnTrueWhenNextHearingDeletedInResultForApplication(){
        final Hearing oldHearingResult = buildHearingWithCourtApplications(
                emptyList(),
                asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withJudicialResults(asList(buildRelatedNextHearingJudicialResult(buildNextHearing())))
                        .build()));

        final Hearing newHearingResult = buildHearingWithCourtApplications(
                emptyList(),
                asList(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withJudicialResults(asList(buildRelatedNextHearingJudicialResult(null)))
                        .build()));

        assertThat(HearingResultHelper.isNextHearingDeleted(newHearingResult, oldHearingResult), is(true));
    }

    private void assertHearing(final HearingListingNeeds hearingListingNeeds, final UUID hearingId, final int size) {
        assertThat(hearingListingNeeds.getId(), is(hearingId));
        assertThat(hearingListingNeeds.getProsecutionCases().size(), is(size));
    }

    private void assertProsecutionCase(final ProsecutionCase prosecutionCase, final UUID prosecutionCaseId, final int size) {
        assertThat(prosecutionCase.getId(), is(prosecutionCaseId));
        assertThat(prosecutionCase.getDefendants().size(), is(size));
        assertThat(prosecutionCase.getCpsOrganisation(), is("A01"));
        assertThat(prosecutionCase.getTrialReceiptType(), is("Transfer"));

    }

    private void assertDefendant(final Defendant defendant, final UUID defendantId, final int size) {
        assertThat(defendant.getId(), is(defendantId));
        assertThat(defendant.getOffences().size(), is(size));
    }

    private void assertOffence(final Offence offence, final UUID offenceId) {
        assertThat(offence.getId(), is(offenceId));
    }

    private void assertReportingRestriction(final ReportingRestriction reportingRestriction, final UUID reportingRestrictionId) {
        assertThat(reportingRestriction.getId(), is(reportingRestrictionId));
    }

    private Hearing buildHearing(final List<ProsecutionCase> prosecutionCases) {
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCases)
                .build();
    }

    private Hearing buildHearingWithCourtApplications(final List<ProsecutionCase> prosecutionCases,
                                                      final List<CourtApplication> courtApplications) {
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCases)
                .withCourtApplications(courtApplications)
                .build();
    }

    private ProsecutionCase buildProsecutionCase(final UUID prosecutionCaseId, final List<Defendant> defendants) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCpsOrganisation("A01")
                .withDefendants(defendants)
                .withTrialReceiptType("Transfer")
                .build();
    }

    private JudicialResult buildJudicialResultWithAmendmentAs(final NextHearing nextHearing, final boolean isNewAmendment) {
        return JudicialResult.judicialResult()
                .withNextHearing(nextHearing)
                .withIsNewAmendment(isNewAmendment)
                .build();
    }

    private JudicialResult buildJudicialResult(final NextHearing nextHearing) {
        return JudicialResult.judicialResult()
                .withNextHearing(nextHearing)
                .withIsNewAmendment(true)
                .build();
    }

    private ReportingRestriction buildReportingRestriction(final UUID id, final UUID judicialResultId, final String label, final LocalDate orderedDate) {
        return ReportingRestriction.reportingRestriction()
                .withId(id)
                .withJudicialResultId(judicialResultId)
                .withLabel(label)
                .withOrderedDate(orderedDate)
                .build();
    }

    private Offence buildOffence(final UUID offenceId, final List<JudicialResult> judicialResults) {
        return Offence.offence()
                .withId(offenceId)
                .withJudicialResults(judicialResults)
                .build();
    }

    private Offence buildOffence(final UUID offenceId, final List<JudicialResult> judicialResults, final List<ReportingRestriction> reportingRestrictions) {
        return Offence.offence()
                .withId(offenceId)
                .withJudicialResults(judicialResults)
                .withReportingRestrictions(reportingRestrictions)
                .build();
    }

    private Defendant buildDefendant(final UUID defendantId, final List<Offence> offences) {
        return buildDefendant(defendantId, offences, null);
    }

    private Defendant buildDefendant(final UUID defendantId, final List<Offence> offences, final List<JudicialResult> judicialResults) {
        return Defendant.defendant()
                .withId(defendantId)
                .withOffences(offences)
                .withDefendantCaseJudicialResults(judicialResults)
                .build();
    }

    private static NextHearing buildNextHearing(final UUID existingHearingId) {
        return NextHearing.nextHearing()
                .withExistingHearingId(existingHearingId)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCode(COMMITTING_COURT_CODE)
                        .withName(COMMITTING_COURT_NAME)
                        .build())
                .build();
    }

    private static NextHearing buildNextHearing() {
        return NextHearing.nextHearing()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCode(COMMITTING_COURT_CODE)
                        .withName(COMMITTING_COURT_NAME)
                        .build())
                .build();
    }

    private static NextHearing buildNextHearingWithNoFixedNextHearingDate(final UUID existingHearingId) {
        return NextHearing.nextHearing()
                .withExistingHearingId(existingHearingId)
                .withDateToBeFixed(true)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCode(COMMITTING_COURT_CODE)
                        .withName(COMMITTING_COURT_NAME)
                        .build())
                .build();
    }

    private CourtApplication buildCourtApplicationsAndUnscheduledJudicialResults(final UUID caseId, final boolean isNewAmendment) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withJudicialResults(asList(buildUnscheduledJudicialResultWithAmendmentAs(buildNextHearing(HEARING_ID_1), isNewAmendment)))
                .build();
    }

    private CourtApplication buildCourtApplicationsWithRelatedNextHearingJudicialResults(final UUID caseId, final boolean isNewAmendment) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withJudicialResults(asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(HEARING_ID_1), isNewAmendment)))
                .build();
    }

    private CourtApplication buildCourtApplicationsWithNewNextHearingJudicialResults(final UUID caseId) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withJudicialResults(asList(buildRelatedNextHearingJudicialResult(buildNextHearing())))
                .build();
    }

    private CourtApplication buildCourtApplicationsWithNoFixedDateOfNextHearing(final UUID caseId) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withJudicialResults(asList(buildUnscheduledJudicialResult(buildNextHearing(HEARING_ID_1))))
                .build();
    }

    private CourtApplication buildCourtApplications(final UUID caseId) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withJudicialResults(asList(buildJudicialResult(buildNextHearing(HEARING_ID_1))))
                .build();
    }

    private CourtApplication buildCourtApplicationsWithoutRelatedNextHearing(final UUID caseId) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withJudicialResults(asList(buildJudicialResult(buildNextHearing())))
                .build();
    }

    private JudicialResult buildUnscheduledJudicialResult(final NextHearing nextHearing) {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(true)
                .withNextHearing(nextHearing)
                .withIsNewAmendment(true)
                .build();
    }

    private JudicialResult buildUnscheduledJudicialResultWithAmendmentAs(final NextHearing nextHearing, final boolean isNewAmendment) {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(true)
                .withNextHearing(nextHearing)
                .withIsNewAmendment(isNewAmendment)
                .build();
    }

    private JudicialResult buildRelatedNextHearingJudicialResult(final NextHearing nextHearing) {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(true)
                .withNextHearing(nextHearing)
                .withIsNewAmendment(true)
                .build();
    }

    private JudicialResult buildRelatedNextHearingJudicialResultWithAmendmentAs(final NextHearing nextHearing, final boolean isNewAmendment) {
        return JudicialResult.judicialResult()
                .withIsUnscheduled(true)
                .withNextHearing(nextHearing)
                .withIsNewAmendment(isNewAmendment)
                .build();
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForOneOffenceForOneDefendantInOneProsecutionCaseWithCourtApplication() {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));

        final Hearing hearing = buildHearingWithCourtApplications(
                asList(buildProsecutionCase(PROSECUTION_CASE_ID_1,
                        of(buildDefendant(DEFENDANT_ID_1,
                                of(buildOffence(OFFENCE_ID_1,
                                        asList(buildUnscheduledJudicialResult(buildNextHearing(HEARING_ID_1))),
                                        singletonList(buildReportingRestriction(REPORTING_RESTRICTION_ID_1, randomUUID(), randomUUID().toString(), LocalDate.now()))))
                                        .collect(Collectors.toList())
                        )).collect(Collectors.toList()))),
                asList(CourtApplication.courtApplication()
                        .withId(PROSECUTION_CASE_ID_1)
                        .withJudicialResults(judicialResults)
                        .build()));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));

        final HearingListingNeeds hearingListingNeeds = nextHearingDetails.getHearingListingNeedsList().get(0);
        assertHearing(hearingListingNeeds, HEARING_ID_1, 1);

        final ProsecutionCase prosecutionCase = hearingListingNeeds.getProsecutionCases().get(0);
        assertProsecutionCase(prosecutionCase, PROSECUTION_CASE_ID_1, 1);

        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertDefendant(defendant, DEFENDANT_ID_1, 1);

        final Offence offence = defendant.getOffences().get(0);
        assertOffence(offence, OFFENCE_ID_1);

        final ReportingRestriction reportingRestriction = offence.getReportingRestrictions().get(0);
        assertReportingRestriction(reportingRestriction, REPORTING_RESTRICTION_ID_1);
    }

    @Test
    public void shouldReturnOneHearingWhenOneNextHearingAvailableForOneOffenceForOneDefendantNoProsecutionCaseWithCourtApplication() {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));
        judicialResults.add(buildJudicialResult(buildNextHearing(HEARING_ID_1)));

        final Hearing hearing = buildHearingWithCourtApplications(null,
                asList(CourtApplication.courtApplication()
                        .withId(PROSECUTION_CASE_ID_1)
                        .withJudicialResults(judicialResults)
                        .build()));

        final NextHearingDetails nextHearingDetails = HearingResultHelper.createRelatedHearings(hearing, false, null, null);
        assertThat(nextHearingDetails.getHearingListingNeedsList().size(), is(1));
    }

    @Test
    public void shouldReturnFalseForApplicationContainsNewNextHearingWhenNextHearingResultsNotAmended_NoDuplicateJudicialResults() {
        final Hearing hearing = buildHearingWithCourtApplications(
                emptyList(),
                asList(buildCourtApplicationsWithCourtOrderRelatedNextHearingJudicialResults(PROSECUTION_CASE_ID_1, false)));

        assertThat(HearingResultHelper.doHearingContainNewOrAmendedNextHearingResults(hearing), is(false));
    }

    private CourtApplication buildCourtApplicationsWithCourtOrderRelatedNextHearingJudicialResults(final UUID caseId, final boolean isNewAmendment) {
        return CourtApplication.courtApplication()
                .withId(caseId)
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(Arrays.asList(CourtOrderOffence.courtOrderOffence()
                                        .withOffence(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(new ArrayList(Arrays.asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(HEARING_ID_1), isNewAmendment))))
                                                .withVerdict(Verdict.verdict().withOffenceId(randomUUID()).build())
                                                .build())
                                        .build(),
                                CourtOrderOffence.courtOrderOffence()
                                        .withOffence(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(new ArrayList(Arrays.asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(HEARING_ID_1), isNewAmendment))))
                                                .withListingNumber(11)
                                                .build())
                                        .build()))
                        .build())
                .withJudicialResults(new ArrayList(Arrays.asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(HEARING_ID_1), isNewAmendment))))
                .build();
    }
}
