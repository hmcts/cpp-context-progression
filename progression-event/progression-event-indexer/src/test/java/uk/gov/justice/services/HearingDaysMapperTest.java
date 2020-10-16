package uk.gov.justice.services;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class HearingDaysMapperTest {

    private HearingDaysMapper hearingDaysMapper;

    @Before
    public void before() {
        hearingDaysMapper = new HearingDaysMapper();
    }

    @Test
    public void shouldReturnEmptyHearingDaysList() {
        final Hearing hearingWithNoHearingDay = Hearing
                .hearing()
                .build();

        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDaysEmpty = hearingDaysMapper.extractHearingDays(hearingWithNoHearingDay);
        assertThat(hearingDaysEmpty, is(notNullValue()));
        assertThat(hearingDaysEmpty, hasSize(0));
    }

    @Test
    public void shouldReturnHearingDays() {

        final ZonedDateTime today = ZonedDateTime.now();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final HearingDay hearingDay = HearingDay
                .hearingDay()
                .withListingSequence(Integer.valueOf(1))
                .withListedDurationMinutes(Integer.valueOf(1))
                .withSittingDay(today)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .build();

        final Hearing hearingWithHearingDays = Hearing
                .hearing()
                .withHearingDays(Arrays.asList(hearingDay))
                .build();

        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDays = hearingDaysMapper.extractHearingDays(hearingWithHearingDays);
        assertThat(hearingDays, is(notNullValue()));
        assertThat(hearingDays, hasSize(1));

        uk.gov.justice.services.unifiedsearch.client.domain.HearingDay result = hearingDays.get(0);
        assertThat(result.getSittingDay(), is(today.format(DateTimeFormatter.ISO_INSTANT)));
        assertThat(fromString(result.getCourtCentreId()),is(courtCentreId));
        assertThat(fromString(result.getCourtRoomId()),is(courtRoomId));
    }

    @Test
    public void shouldReturnHearingDaysWithCourtCentreEvenIfHearingDaysWithoutCourtCentre() {

        final ZonedDateTime today = ZonedDateTime.now();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final HearingDay hearingDay = HearingDay
                .hearingDay()
                .withListingSequence(Integer.valueOf(1))
                .withListedDurationMinutes(Integer.valueOf(1))
                .withSittingDay(today)
                .build();

        final Hearing hearingWithHearingDays = Hearing
                .hearing()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withRoomId(courtRoomId)
                        .build())
                .withHearingDays(Arrays.asList(hearingDay))
                .build();

        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDays = hearingDaysMapper.extractHearingDays(hearingWithHearingDays);
        assertThat(hearingDays, is(notNullValue()));
        assertThat(hearingDays, hasSize(1));

        uk.gov.justice.services.unifiedsearch.client.domain.HearingDay result = hearingDays.get(0);
        assertThat(result.getSittingDay(), is(today.format(DateTimeFormatter.ISO_INSTANT)));
        assertThat(fromString(result.getCourtCentreId()),is(courtCentreId));
        assertThat(fromString(result.getCourtRoomId()),is(courtRoomId));
    }
}