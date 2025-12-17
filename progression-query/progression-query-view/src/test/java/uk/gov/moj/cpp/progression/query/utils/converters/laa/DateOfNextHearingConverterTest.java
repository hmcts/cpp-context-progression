package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateOfNextHearingConverterTest {

    @InjectMocks
    private DateOfNextHearingConverter dateOfNextHearingConverter;

    @Test
    void shouldReturnNullWhenHearingListIsNull() {
        final String result = dateOfNextHearingConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenHearingListIsEmpty() {
        final String result = dateOfNextHearingConverter.convert(emptyList());

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenHearingDaysIsNull() {

        final List<Hearing> hearingList = singletonList(Hearing.hearing()
                .withHearingDays(null)
                .build());

        final String result = dateOfNextHearingConverter.convert(hearingList);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenSittingDaysIsNull() {

        final List<Hearing> hearingList = singletonList(Hearing.hearing()
                .withHearingDays(singletonList(HearingDay.hearingDay()
                        .withSittingDay(null)
                        .build()))
                .build());

        final String result = dateOfNextHearingConverter.convert(hearingList);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnTheNearestFutureDay() {

        final ZonedDateTime now = ZonedDateTime.now();

        final List<Hearing> hearingList = singletonList(Hearing.hearing()
                .withHearingDays(asList(
                                HearingDay.hearingDay()
                                        .withSittingDay(now.plusDays(5))
                                        .build(),
                                HearingDay.hearingDay()
                                        .withSittingDay(now.plusDays(7))
                                        .build(),
                                HearingDay.hearingDay()
                                        .withSittingDay(now.plusDays(2))
                                        .build(),
                                HearingDay.hearingDay()
                                        .withSittingDay(now.minusDays(3))
                                        .build()
                        )
                )
                .build());

        final String result = dateOfNextHearingConverter.convert(hearingList);

        assertThat(result, is(formatSittingDay(now.plusDays(2))));
    }


    @Test
    void shouldReturnTheMostRecentDateWhenThereIsNoFutureDate() {

        final ZonedDateTime now = ZonedDateTime.now();

        final List<Hearing> hearingList = singletonList(Hearing.hearing()
                .withHearingDays(asList(
                                HearingDay.hearingDay()
                                        .withSittingDay(now.minusDays(5))
                                        .build(),
                                HearingDay.hearingDay()
                                        .withSittingDay(now.minusDays(1))
                                        .build(),
                                HearingDay.hearingDay()
                                        .withSittingDay(now.minusDays(2))
                                        .build(),
                                HearingDay.hearingDay()
                                        .withSittingDay(now.minusDays(3))
                                        .build()
                        )
                )
                .build());

        final String result = dateOfNextHearingConverter.convert(hearingList);

        assertThat(result, is(formatSittingDay(now.minusDays(1))));
    }

    private static String formatSittingDay(final ZonedDateTime sittingDay) {
        return sittingDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }


}