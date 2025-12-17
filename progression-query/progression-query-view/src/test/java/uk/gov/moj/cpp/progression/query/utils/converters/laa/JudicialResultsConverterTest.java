package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.external.ApiJudicialResult;
import uk.gov.justice.progression.query.laa.CaseSummary;
import uk.gov.justice.progression.query.laa.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JudicialResultsConverterTest {

    @InjectMocks
    private JudicialResultsConverter judicialResultsConverter;

    @Test
    void shouldReturnNullWhenJudicialResultsIsEmpty() {
        final List<uk.gov.justice.core.courts.JudicialResult> judicialResults = emptyList();

        final List<ApiJudicialResult> result = judicialResultsConverter.convert(judicialResults);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenJudicialResultsIsNull() {
        final List<uk.gov.justice.core.courts.JudicialResult> judicialResults = null;

        final List<ApiJudicialResult> result = judicialResultsConverter.convert(judicialResults);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertJudicialResultsToApiJudicialResults() {
        final uk.gov.justice.core.courts.JudicialResult judicialResult = uk.gov.justice.core.courts.JudicialResult.judicialResult()
                .withJudicialResultId(randomUUID())
                .withIsAdjournmentResult(false)
                .withIsFinancialResult(false)
                .withIsConvictedResult(false)
                .withIsAvailableForCourtExtract(false)
                .withOrderedHearingId(randomUUID())
                .withLabel("label")
                .withResultText("result text")
                .withCjsCode("cjs code")
                .withRank(BigDecimal.valueOf(1))
                .withOrderedDate(LocalDate.now())
                .withLastSharedDateTime(LocalDate.now().toString())
                .withTerminatesOffenceProceedings(false)
                .withCategory(JudicialResultCategory.FINAL)
                .build();
        final List<uk.gov.justice.core.courts.JudicialResult> judicialResults = singletonList(judicialResult);

        final List<ApiJudicialResult> result = judicialResultsConverter.convert(judicialResults);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getJudicialResultId(), is(judicialResult.getJudicialResultId()));
        assertThat(result.get(0).getIsAdjournmentResult(), is(judicialResult.getIsAdjournmentResult()));
        assertThat(result.get(0).getIsFinancialResult(), is(judicialResult.getIsFinancialResult()));
        assertThat(result.get(0).getIsConvictedResult(), is(judicialResult.getIsConvictedResult()));
        assertThat(result.get(0).getIsAvailableForCourtExtract(), is(judicialResult.getIsAvailableForCourtExtract()));
        assertThat(result.get(0).getOrderedHearingId(), is(judicialResult.getOrderedHearingId()));
        assertThat(result.get(0).getLabel(), is(judicialResult.getLabel()));
        assertThat(result.get(0).getResultText(), is(judicialResult.getResultText()));
        assertThat(result.get(0).getCjsCode(), is(judicialResult.getCjsCode()));
        assertThat(result.get(0).getRank(), is(judicialResult.getRank()));
        assertThat(result.get(0).getOrderedDate(), is(judicialResult.getOrderedDate()));
        assertThat(result.get(0).getLastSharedDateTime(), is(judicialResult.getLastSharedDateTime()));
        assertThat(result.get(0).getTerminatesOffenceProceedings(), is(judicialResult.getTerminatesOffenceProceedings()));
        assertThat(result.get(0).getCategory().toString(), is(judicialResult.getCategory().toString()));
    }

    @Test
    void shouldConvertJudicialResultsToApiJudicialResultsWhenThereAreNullValues() {
        final uk.gov.justice.core.courts.JudicialResult judicialResult = uk.gov.justice.core.courts.JudicialResult.judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(null)
                .build();
        final List<uk.gov.justice.core.courts.JudicialResult> judicialResults = singletonList(judicialResult);

        final List<ApiJudicialResult> result = judicialResultsConverter.convert(judicialResults);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getJudicialResultId(), is(judicialResult.getJudicialResultId()));
        assertThat(result.get(0).getCategory(), nullValue());
    }

}