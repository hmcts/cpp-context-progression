package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.query.laa.CourtCentre;
import uk.gov.justice.progression.query.laa.OffenceSummary;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffenceSummaryConverterTest {

    @InjectMocks
    private OffenceSummaryConverter offenceSummaryConverter;

    @Mock
    private LaaApplnReferenceConverter laaApplnReferenceConverter;

    @Test
    void shouldReturnNullWhenCourtApplicationCaseIsNull() {
        final List<OffenceSummary> result = offenceSummaryConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenCourtApplicationCaseIsEmpty() {
        final List<OffenceSummary> result = offenceSummaryConverter.convert(emptyList());

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertToOffenceSummary() {
        final List<CourtApplicationCase> courtApplicationCases = List.of(
                CourtApplicationCase.courtApplicationCase()
                        .withOffences(Arrays.asList(Offence.offence()
                                        .withOffenceCode("code")
                                        .withId(randomUUID())
                                        .withArrestDate(LocalDate.now())
                                        .withChargeDate(LocalDate.now())
                                        .withDateOfInformation(LocalDate.now())
                                        .withEndDate(LocalDate.now())
                                        .withModeOfTrial("mode of trial")
                                        .withOffenceLegislation("legislation")
                                        .withOffenceTitle("title")
                                        .withOrderIndex(1)
                                        .withProceedingsConcluded(false)
                                        .withStartDate(LocalDate.now())
                                        .withWording("wording")
                                .build()))
                        .build());

        final List<OffenceSummary> result = offenceSummaryConverter.convert(courtApplicationCases);

        assertThat(result.get(0).getOffenceCode(), is(courtApplicationCases.get(0).getOffences().get(0).getOffenceCode()));
        assertThat(result.get(0).getOffenceId(), is(courtApplicationCases.get(0).getOffences().get(0).getId()));
        assertThat(result.get(0).getArrestDate(), is(courtApplicationCases.get(0).getOffences().get(0).getArrestDate()));
        assertThat(result.get(0).getChargeDate(), is(courtApplicationCases.get(0).getOffences().get(0).getChargeDate()));
        assertThat(result.get(0).getDateOfInformation(), is(courtApplicationCases.get(0).getOffences().get(0).getDateOfInformation()));
        assertThat(result.get(0).getEndDate(), is(courtApplicationCases.get(0).getOffences().get(0).getEndDate()));
        assertThat(result.get(0).getModeOfTrial(), is(courtApplicationCases.get(0).getOffences().get(0).getModeOfTrial()));
        assertThat(result.get(0).getOffenceLegislation(), is(courtApplicationCases.get(0).getOffences().get(0).getOffenceLegislation()));
        assertThat(result.get(0).getOffenceTitle(), is(courtApplicationCases.get(0).getOffences().get(0).getOffenceTitle()));
        assertThat(result.get(0).getOrderIndex(), is(courtApplicationCases.get(0).getOffences().get(0).getOrderIndex()));
        assertThat(result.get(0).getProceedingsConcluded(), is(courtApplicationCases.get(0).getOffences().get(0).getProceedingsConcluded()));
        assertThat(result.get(0).getStartDate(), is(courtApplicationCases.get(0).getOffences().get(0).getStartDate()));
        assertThat(result.get(0).getWording(), is(courtApplicationCases.get(0).getOffences().get(0).getWording()));
    }

    @Test
    void shouldConvertToOffenceSummaryWhenThereAreNullValues() {
        final List<CourtApplicationCase> courtApplicationCases = List.of(
                CourtApplicationCase.courtApplicationCase()
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceCode("code")
                                .withId(randomUUID())
                                .withArrestDate(null)
                                .withChargeDate(null)
                                .withDateOfInformation(null)
                                .withEndDate(null)
                                .withModeOfTrial("mode of trial")
                                .withOffenceLegislation("legislation")
                                .withOffenceTitle("title")
                                .withOrderIndex(1)
                                .withProceedingsConcluded(false)
                                .withStartDate(null)
                                .withWording("wording")
                                .build()))
                        .build());

        final List<OffenceSummary> result = offenceSummaryConverter.convert(courtApplicationCases);

        assertThat(result.get(0).getOffenceCode(), is(courtApplicationCases.get(0).getOffences().get(0).getOffenceCode()));
        assertThat(result.get(0).getOffenceId(), is(courtApplicationCases.get(0).getOffences().get(0).getId()));
        assertThat(result.get(0).getArrestDate(), nullValue());
        assertThat(result.get(0).getChargeDate(),  nullValue());
        assertThat(result.get(0).getDateOfInformation(), nullValue());
        assertThat(result.get(0).getEndDate(), nullValue());
        assertThat(result.get(0).getModeOfTrial(), is(courtApplicationCases.get(0).getOffences().get(0).getModeOfTrial()));
        assertThat(result.get(0).getOffenceLegislation(), is(courtApplicationCases.get(0).getOffences().get(0).getOffenceLegislation()));
        assertThat(result.get(0).getOffenceTitle(), is(courtApplicationCases.get(0).getOffences().get(0).getOffenceTitle()));
        assertThat(result.get(0).getOrderIndex(), is(courtApplicationCases.get(0).getOffences().get(0).getOrderIndex()));
        assertThat(result.get(0).getProceedingsConcluded(), is(courtApplicationCases.get(0).getOffences().get(0).getProceedingsConcluded()));
        assertThat(result.get(0).getStartDate(), nullValue());
        assertThat(result.get(0).getWording(), is(courtApplicationCases.get(0).getOffences().get(0).getWording()));
    }

}