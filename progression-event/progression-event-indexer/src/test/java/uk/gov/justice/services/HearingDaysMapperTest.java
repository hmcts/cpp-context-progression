package uk.gov.justice.services;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.DomainToIndexMapper.ISO_8601_FORMATTER;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingDaysMapperTest {

    private HearingDaysMapper hearingDaysMapper;

    @BeforeEach
    public void before() {
        hearingDaysMapper = new HearingDaysMapper(new HearingDaySharedResultsMapper());
    }

    @Test
    public void shouldReturnEmptyHearingDaysList() {
        final Hearing hearingWithNoHearingDay = Hearing
                .hearing()
                .withProsecutionCases(Arrays.asList(getProsecutionCaseWithOrderedDateSameAsHearingDay()))
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
                .withHasSharedResults(true)
                .build();

        final Hearing hearingWithHearingDays = Hearing.hearing()
                .withHearingDays(Arrays.asList(hearingDay))
                .withProsecutionCases(Arrays.asList(getProsecutionCaseWithOrderedDateSameAsHearingDay())).build();


        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDays = hearingDaysMapper.extractHearingDays(hearingWithHearingDays);
        assertThat(hearingDays, is(notNullValue()));
        assertThat(hearingDays, hasSize(1));

        uk.gov.justice.services.unifiedsearch.client.domain.HearingDay result = hearingDays.get(0);
        assertThat(result.getSittingDay(), is(today.format(ISO_8601_FORMATTER)));
        assertThat(fromString(result.getCourtCentreId()), is(courtCentreId));
        assertThat(fromString(result.getCourtRoomId()), is(courtRoomId));
        assertThat(result.getHasSharedResults(), is(true));
    }


    @Test
    public void shouldReturnHearingDaysWithCourtCentreEvenIfHearingDaysWithoutCourtCentreAndSameSittingAndHearingDate() {

        final ZonedDateTime today = ZonedDateTime.now();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final HearingDay hearingDay = HearingDay
                .hearingDay()
                .withListingSequence(Integer.valueOf(1))
                .withListedDurationMinutes(Integer.valueOf(1))
                .withSittingDay(today)
                .withHasSharedResults(false)
                .build();
        final Hearing hearingWithHearingDays = Hearing.hearing()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withRoomId(courtRoomId)
                        .build())
                .withHearingDays(Arrays.asList(hearingDay))
                .withProsecutionCases(Arrays.asList(getProsecutionCaseWithOrderedDateSameAsHearingDay())).build();

        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDays = hearingDaysMapper.extractHearingDays(hearingWithHearingDays);
        assertThat(hearingDays, is(notNullValue()));
        assertThat(hearingDays, hasSize(1));

        uk.gov.justice.services.unifiedsearch.client.domain.HearingDay result = hearingDays.get(0);
        assertThat(result.getSittingDay(), is(today.format(ISO_8601_FORMATTER)));
        assertThat(fromString(result.getCourtCentreId()), is(courtCentreId));
        assertThat(fromString(result.getCourtRoomId()), is(courtRoomId));
        assertThat(result.getHasSharedResults(), is(true));
    }

    @Test
    public void shouldReturnHearingDaysWithCourtCentreEvenIfHearingDaysWithoutCourtCentreAndDifferentSittingAndHearingDate() {

        final ZonedDateTime today = ZonedDateTime.now();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final HearingDay hearingDay = HearingDay
                .hearingDay()
                .withListingSequence(Integer.valueOf(1))
                .withListedDurationMinutes(Integer.valueOf(1))
                .withSittingDay(today)
                .withHasSharedResults(false)
                .build();
        final Hearing hearingWithHearingDays = Hearing.hearing()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withRoomId(courtRoomId)
                        .build())
                .withHearingDays(Arrays.asList(hearingDay))
                .withProsecutionCases(Arrays.asList(getProsecutionCaseWithOrderedDateDifferentThanHearingDay())).build();

        final List<uk.gov.justice.services.unifiedsearch.client.domain.HearingDay> hearingDays = hearingDaysMapper.extractHearingDays(hearingWithHearingDays);
        assertThat(hearingDays, is(notNullValue()));
        assertThat(hearingDays, hasSize(1));

        uk.gov.justice.services.unifiedsearch.client.domain.HearingDay result = hearingDays.get(0);
        assertThat(result.getSittingDay(), is(today.format(ISO_8601_FORMATTER)));
        assertThat(fromString(result.getCourtCentreId()), is(courtCentreId));
        assertThat(fromString(result.getCourtRoomId()), is(courtRoomId));
        assertThat(result.getHasSharedResults(), is(false));
    }


    private ProsecutionCase getProsecutionCaseWithOrderedDateSameAsHearingDay() {
        final JudicialResult judicialResult1 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now()).build();
        final JudicialResult judicialResult2 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final Offence offence = Offence.offence().withJudicialResults(Arrays.asList(judicialResult1, judicialResult2)).build();

        final Defendant defendant = Defendant.defendant().withOffences(Arrays.asList(offence)).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(Arrays.asList(defendant)).build();
        return prosecutionCase;
    }

    private ProsecutionCase getProsecutionCaseWithOrderedDateDifferentThanHearingDay() {
        final JudicialResult judicialResult1 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(1)).build();
        final JudicialResult judicialResult2 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final Offence offence = Offence.offence().withJudicialResults(Arrays.asList(judicialResult1, judicialResult2)).build();

        final Defendant defendant = Defendant.defendant().withOffences(Arrays.asList(offence)).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(Arrays.asList(defendant)).build();
        return prosecutionCase;
    }

}