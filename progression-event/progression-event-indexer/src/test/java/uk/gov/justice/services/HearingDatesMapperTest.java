package uk.gov.justice.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import org.junit.Before;
import org.junit.Test;

public class HearingDatesMapperTest {

    private HearingDatesMapper hearingDatesMapper;

    @Before
    public void before() {
        hearingDatesMapper = new HearingDatesMapper();
    }

    @Test
    public void shouldReturnEmptyHearingDatesList() {
        final Hearing hearingWithNoHearingDay = Hearing
                .hearing()
                .build();

        final List<String> hearingDatesEmpty = hearingDatesMapper.extractHearingDates(hearingWithNoHearingDay);
        assertThat(hearingDatesEmpty, is(notNullValue()));
        assertThat(hearingDatesEmpty, hasSize(0));
    }

    @Test
    public void shouldReturnHearingDates() {

        final ZonedDateTime today = ZonedDateTime.now();

        final HearingDay hearingDay = HearingDay
                .hearingDay()
                .withSittingDay(today)
                .build();

        final Hearing hearingWithHearingDays = Hearing
                .hearing()
                .withHearingDays(Arrays.asList(hearingDay))
                .build();

        final List<String> hearingDates = hearingDatesMapper.extractHearingDates(hearingWithHearingDays);
        assertThat(hearingDates, is(notNullValue()));
        assertThat(hearingDates, hasSize(1));
    }
}