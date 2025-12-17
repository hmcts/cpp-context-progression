package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.progression.query.laa.HearingDay;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingDayConverterTest {

    @InjectMocks
    private HearingDayConverter hearingDayConverter;

    @Test
    void shouldReturnNullWhenHearingDaysIsNull() {
        final List<HearingDay> result = hearingDayConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenHearingDaysIsEmpty() {
        final List<HearingDay> result = hearingDayConverter.convert(emptyList());

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertHearingDay() {
        final List<uk.gov.justice.core.courts.HearingDay> hearingDays = List.of(
                uk.gov.justice.core.courts.HearingDay.hearingDay()
                        .withHasSharedResults(true)
                        .withSittingDay(ZonedDateTime.now())
                        .withListingSequence(1)
                        .withListedDurationMinutes(10)
                        .build(),
                uk.gov.justice.core.courts.HearingDay.hearingDay()
                        .withHasSharedResults(false)
                        .withListingSequence(2)
                        .withListedDurationMinutes(20)
                        .build()
        );

        final List<HearingDay> result = hearingDayConverter.convert(hearingDays);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getHasSharedResults(), is(hearingDays.get(0).getHasSharedResults()));
        assertThat(result.get(0).getListedDurationMinutes(), is(hearingDays.get(0).getListedDurationMinutes()));
        assertThat(result.get(0).getListingSequence(), is(hearingDays.get(0).getListingSequence()));
        assertThat(result.get(0).getSittingDay(), is(formatSittingDay(hearingDays.get(0).getSittingDay())));

        assertThat(result.get(1).getHasSharedResults(), is(hearingDays.get(1).getHasSharedResults()));
        assertThat(result.get(1).getListedDurationMinutes(), is(hearingDays.get(1).getListedDurationMinutes()));
        assertThat(result.get(1).getListingSequence(), is(hearingDays.get(1).getListingSequence()));
        assertThat(result.get(1).getSittingDay(), nullValue());
    }

    private static String formatSittingDay(final ZonedDateTime sittingDay) {
        return sittingDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }


}