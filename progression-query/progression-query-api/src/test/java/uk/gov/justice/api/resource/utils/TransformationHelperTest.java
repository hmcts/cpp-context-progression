package uk.gov.justice.api.resource.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.api.resource.utils.TransformationHelper.getHearingsSortedByHearingDaysAsc;

import uk.gov.justice.progression.courts.exract.HearingDays;
import uk.gov.justice.progression.courts.exract.Hearings;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TransformationHelperTest {

    TransformationHelper transformationHelper = new TransformationHelper();

    @Test
    public void givenSingleHearingsWithSingleHearingDay_shouldSortHearingsAsc() {

        final LocalDate sittingDay = LocalDate.now();
        final List<Hearings> hearings = singletonList(
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay));
    }

    @Test
    public void givenSingleHearingsWithMultipleHearingDays_shouldSortHearingsAsc() {

        final LocalDate sittingDay01 = LocalDate.now();
        final LocalDate sittingDay02 = sittingDay01.plusDays(2);
        final List<Hearings> hearings = singletonList(
                Hearings.hearings()
                        .withHearingDays(asList(HearingDays.hearingDays().withDay(sittingDay01).build(),
                                HearingDays.hearingDays().withDay(sittingDay02).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay01));
    }

    @Test
    public void givenTwoHearingsWithFirstHearingDaysInFuture_shouldSortHearingsWithHearingDaysAsc() {
        final LocalDate sittingDay01 = LocalDate.now();
        final LocalDate sittingDay02 = sittingDay01.plusDays(2);
        final List<Hearings> hearings = asList(
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay01.plusDays(10)).build()))
                        .build(),
                Hearings.hearings()
                        .withHearingDays(asList(HearingDays.hearingDays().withDay(sittingDay01).build(),
                                HearingDays.hearingDays().withDay(sittingDay02).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay01));
    }

    @Test
    public void givenTwoHearingsWithSecondHearingDaysInFuture_shouldSortHearingsWithHearingDaysAsc() {
        final LocalDate sittingDay01 = LocalDate.now();
        final LocalDate sittingDay02 = sittingDay01.plusDays(2);
        final List<Hearings> hearings = asList(
                Hearings.hearings()
                        .withHearingDays(asList(HearingDays.hearingDays().withDay(sittingDay01).build(),
                                HearingDays.hearingDays().withDay(sittingDay02).build()))
                        .build(),
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay01.plusDays(10)).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay01));
    }

    @Test
    public void givenTwoHearingsWithEarliestHearingDaysAddedLast_shouldSortHearingsWithHearingDaysAsc() {
        final LocalDate sittingDay01 = LocalDate.now();
        final LocalDate sittingDay02 = sittingDay01.plusDays(2);
        final LocalDate sittingDay03 = sittingDay01.plusDays(3);

        final List<Hearings> hearings = asList(
                Hearings.hearings()
                        .withHearingDays(asList(HearingDays.hearingDays().withDay(sittingDay01).build(),
                                HearingDays.hearingDays().withDay(sittingDay02).build()))
                        .build(),
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay03).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay01));
    }

    @Test
    public void givenTwoHearingsWithEarliestHearingDaysAddedFirst_shouldSortHearingsWithHearingDaysAsc() {
        final LocalDate sittingDay01 = LocalDate.now();
        final LocalDate sittingDay02 = sittingDay01.plusDays(2);
        final LocalDate sittingDay03 = sittingDay01.plusDays(3);

        final List<Hearings> hearings = asList(
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay03).build()))
                        .build(),
                Hearings.hearings()
                        .withHearingDays(asList(HearingDays.hearingDays().withDay(sittingDay01).build(),
                                HearingDays.hearingDays().withDay(sittingDay02).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay01));
    }

    @Test
    public void givenMultipleHearingsWithEarliestHearingDaysAddedMiddle_shouldSortHearingsWithHearingDaysAsc() {
        final LocalDate sittingDay01 = LocalDate.now();
        final LocalDate sittingDay02 = sittingDay01.plusDays(2);
        final LocalDate sittingDay03 = sittingDay01.plusDays(3);
        final LocalDate sittingDay04 = sittingDay01.plusDays(4);

        final List<Hearings> hearings = asList(
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay03).build()))
                        .build(),
                Hearings.hearings()
                        .withHearingDays(singletonList(HearingDays.hearingDays().withDay(sittingDay04).build()))
                        .build(),
                Hearings.hearings()
                        .withHearingDays(asList(HearingDays.hearingDays().withDay(sittingDay01).build(),
                                HearingDays.hearingDays().withDay(sittingDay02).build()))
                        .build());

        final List<Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(hearings);

        assertThat(sortedHearings.get(0).getHearingDays().get(0).getDay(), is(sittingDay01));
    }
}