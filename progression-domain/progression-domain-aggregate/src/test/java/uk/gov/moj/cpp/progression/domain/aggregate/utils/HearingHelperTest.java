package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingHelperTest {

    private static final UtcClock clock = new UtcClock();

    @Test
    public void shouldBeEligibleForNextHearingsWhenEarliestNextHearingDateIsNowButIsASingleDayHearing() {
        final ZonedDateTime now = clock.now();

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(HearingDay
                        .hearingDay()
                        .withSittingDay(now)
                        .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(true));
    }

    // --------- Multi-Day Hearings ----------

    @Test
    public void shouldBeEligibleForNextHearingsWhenNoEarliestNextHearingDate() {
        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .build(),
                        HearingDay.hearingDay()
                                .build()))
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(true));
    }


    @Test
    public void shouldNotBeEligibleForNextHearingsWhenEarliestNextHearingDateIsNow() {
        final ZonedDateTime now = clock.now();

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(false));
    }

    @Test
    public void shouldNotBeEligibleForNextHearingsWhenEarliestNextHearingDateIs1HourInFuture() {
        final ZonedDateTime now = (new UtcClock()).now();

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now.plusHours(1))
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(false));
    }

    @Test
    public void shouldNotBeEligibleForNextHearingsWhenEarliestNextHearingDateIs1HourInPast() {
        final ZonedDateTime now = clock.now().minusHours(1);

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(false));
    }

    @Test
    public void shouldBeEligibleForNextHearingsWhenEarliestNextHearingDateIs1DayInFuture() {
        final ZonedDateTime now = clock.now().plusDays(1);

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(true));
    }

    @Test
    public void shouldNotBeEligibleForNextHearingsWhenEarliestNextHearingDateIs1DayInPast() {
        final ZonedDateTime now = clock.now().minusDays(1);

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(false));
    }

    @Test
    public void shouldBeEligibleForNextHearingsWhenEarliestNextHearingDateIs3DaysInFuture() {
        final ZonedDateTime now = clock.now().plusDays(3);

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(true));
    }

    @Test
    public void shouldNotBeEligibleForNextHearingsWhenEarliestNextHearingDateIs3DaysInPast() {
        final ZonedDateTime now = clock.now().minusDays(1);

        final Hearing hearing = Hearing.hearing()
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(now)
                                .build(),
                        HearingDay.hearingDay()
                                .withSittingDay(now.plusDays(1))
                                .build()))
                .withEarliestNextHearingDate(now)
                .build();

        final boolean eligibleForNextHearings = HearingHelper.isEligibleForNextHearings(hearing);

        assertThat(eligibleForNextHearings, is(false));
    }

}
