package uk.gov.justice.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.JsonHelper.readJson;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("squid:S2187")
@ExtendWith(MockitoExtension.class)
public class HearingDaySharedResultsMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @InjectMocks
    private HearingDaySharedResultsMapper hearingDaySharedResultsMapper;

    @Test
    public void shouldSetHearingDayWithCasesOnlySharedResultsAsTrue() {
        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();

        final JudicialResult judicialResult1 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now()).build();
        final JudicialResult judicialResult2 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final Offence offence = Offence.offence().withJudicialResults(Arrays.asList(judicialResult1, judicialResult2)).build();

        final Defendant defendant = Defendant.defendant().withOffences(Arrays.asList(offence)).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(Arrays.asList(defendant)).build();

        final Hearing hearing = Hearing.hearing().withHearingDays(Arrays.asList(hearingDay)).withProsecutionCases(Arrays.asList(prosecutionCase)).build();

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(true));
    }

    @Test
    public void shouldSetHearingDayWithCasesOnlySharedResultsAsFalse() {
        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();

        final JudicialResult judicialResult1 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final JudicialResult judicialResult2 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final Offence offence = Offence.offence().withJudicialResults(Arrays.asList(judicialResult1, judicialResult2)).build();

        final Defendant defendant = Defendant.defendant().withOffences(Arrays.asList(offence)).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(Arrays.asList(defendant)).build();
        final Hearing hearing = Hearing.hearing().withHearingDays(Arrays.asList(hearingDay)).withProsecutionCases(Arrays.asList(prosecutionCase)).build();

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(false));
    }

    @Test
    public void shouldSetHearingDayWithCourtApplicationsOnlySharedResultsAsTrue() {
        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();

        final JudicialResult judicialResult1 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now()).build();
        final JudicialResult judicialResult2 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final CourtApplication courtApplication = CourtApplication.courtApplication().withJudicialResults(Arrays.asList(judicialResult1, judicialResult2)).build();
        final Hearing hearing = Hearing.hearing().withHearingDays(Arrays.asList(hearingDay)).withCourtApplications(Arrays.asList(courtApplication)).build();

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(true));
    }

    @Test
    public void shouldSetHearingDayWithCourtApplicationsOnlySharedResultsAsFalse() {
        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();

        final JudicialResult judicialResult1 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final JudicialResult judicialResult2 = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();

        final CourtApplication courtApplication = CourtApplication.courtApplication().withJudicialResults(Arrays.asList(judicialResult1, judicialResult2)).build();
        final Hearing hearing = Hearing.hearing().withHearingDays(Arrays.asList(hearingDay)).withCourtApplications(Arrays.asList(courtApplication)).build();

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(false));
    }

    @Test
    public void shouldSetHearingDayWithCourtApplicationsAndCasesSharedResultsAsTrue() {

        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();

        final JudicialResult judicialResult1Application = JudicialResult.judicialResult().withOrderedDate(LocalDate.now()).build();
        final JudicialResult judicialResult2Case = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final CourtApplication courtApplication = CourtApplication.courtApplication().withJudicialResults(Arrays.asList(judicialResult1Application)).build();
        final Offence offence = Offence.offence().withJudicialResults(Arrays.asList(judicialResult2Case)).build();

        final Defendant defendant = Defendant.defendant().withOffences(Arrays.asList(offence)).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(Arrays.asList(defendant)).build();
        final Hearing hearing = Hearing.hearing().withHearingDays(Arrays.asList(hearingDay))
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .withCourtApplications(Arrays.asList(courtApplication))
                .build();

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(true));
    }

    @Test
    public void shouldSetHearingDayWithCourtApplicationsAndCasesSharedResultsAsFalse() {
        final HearingDay hearingDay = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(3)).build();

        final JudicialResult judicialResult1Application = JudicialResult.judicialResult().withOrderedDate(LocalDate.now()).build();
        final JudicialResult judicialResult2Case = JudicialResult.judicialResult().withOrderedDate(LocalDate.now().minusDays(2)).build();
        final CourtApplication courtApplication = CourtApplication.courtApplication().withJudicialResults(Arrays.asList(judicialResult1Application)).build();
        final Offence offence = Offence.offence().withJudicialResults(Arrays.asList(judicialResult2Case)).build();

        final Defendant defendant = Defendant.defendant().withOffences(Arrays.asList(offence)).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(Arrays.asList(defendant)).build();
        final Hearing hearing = Hearing.hearing().withHearingDays(Arrays.asList(hearingDay))
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .withCourtApplications(Arrays.asList(courtApplication))
                .build();

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(false));
    }

    @Test
    public void shouldSetHearingDaySharedResultsAsFalseForSingleDayHearing() {
        final JsonObject inputJson = readJson("/judicial-result-ordered-on-different-day.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(inputJson.getJsonObject("hearing"), Hearing.class);

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(false));
    }

    @Test
    public void shouldSetHearingDaySharedResultsAsTrueForSingleDayHearing() {
        final JsonObject inputJson = readJson("/judicial-result-ordered-on-same-day.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(inputJson.getJsonObject("hearing"), Hearing.class);

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(true));
    }

    @Test
    public void shouldSetHearingDaySharedResultsAsTrueForMultiDayHearing() {
        final JsonObject inputJson = readJson("/multi-day-hearing-judicial-result-ordered-on-same-day.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(inputJson.getJsonObject("hearing"), Hearing.class);

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(true));
    }

    @Test
    public void shouldSetHearingDaySharedResultsAsFalseForMultiDayHearing() {
        final JsonObject inputJson = readJson("/multi-day-hearing-judicial-result-ordered-on-different-day.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(inputJson.getJsonObject("hearing"), Hearing.class);

        final boolean hearingDaySharedResults = hearingDaySharedResultsMapper.shouldSetHasSharedResults(hearing);
        assertThat(hearingDaySharedResults, CoreMatchers.is(false));
    }
}