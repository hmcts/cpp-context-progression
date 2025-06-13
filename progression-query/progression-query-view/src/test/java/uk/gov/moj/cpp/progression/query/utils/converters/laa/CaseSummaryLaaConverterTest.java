package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.progression.query.laa.CaseSummary;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseSummaryLaaConverterTest {

    @InjectMocks
    private CaseSummaryLaaConverter caseSummaryLaaConverter;

    @Test
    void shouldReturnNullWhenCourtApplicationCasesIsEmpty() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(emptyList())
                .build();

        final List<CaseSummary> result = caseSummaryLaaConverter.convert(courtApplication);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenCourtApplicationCasesIsNull() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(null)
                .build();

        final List<CaseSummary> result = caseSummaryLaaConverter.convert(courtApplication);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertCourtApplicationCasesToCaseSummary() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(asList(uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase()
                                .withCaseStatus("case status")
                                .withProsecutionCaseId(randomUUID())
                                .withProsecutionCaseIdentifier(uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                        .withCaseURN("URN1")
                                        .build())
                                .build(),
                        uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase()
                                .withCaseStatus("case status")
                                .withProsecutionCaseId(randomUUID())
                                .withProsecutionCaseIdentifier(uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                        .withCaseURN("URN2")
                                        .build())
                                .build()
                ))
                .build();

        final List<CaseSummary> result = caseSummaryLaaConverter.convert(courtApplication);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getCaseStatus(), is(courtApplication.getCourtApplicationCases().get(0).getCaseStatus()));
        assertThat(result.get(0).getProsecutionCaseId(), is(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId()));
        assertThat(result.get(0).getProsecutionCaseReference(), is(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseIdentifier().getCaseURN()));

        assertThat(result.get(1).getCaseStatus(), is(courtApplication.getCourtApplicationCases().get(1).getCaseStatus()));
        assertThat(result.get(1).getProsecutionCaseId(), is(courtApplication.getCourtApplicationCases().get(1).getProsecutionCaseId()));
        assertThat(result.get(1).getProsecutionCaseReference(), is(courtApplication.getCourtApplicationCases().get(1).getProsecutionCaseIdentifier().getCaseURN()));
    }

    @Test
    void shouldConvertCourtApplicationCasesToCaseSummaryWhenThereAreNullValues() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(asList(uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase()
                                .withCaseStatus("case status")
                                .withProsecutionCaseId(randomUUID())
                                .build(),
                        uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase()
                                .withCaseStatus("case status")
                                .withProsecutionCaseId(randomUUID())
                                .withProsecutionCaseIdentifier(uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                        .withCaseURN("URN2")
                                        .build())
                                .build()
                ))
                .build();

        final List<CaseSummary> result = caseSummaryLaaConverter.convert(courtApplication);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getCaseStatus(), is(courtApplication.getCourtApplicationCases().get(0).getCaseStatus()));
        assertThat(result.get(0).getProsecutionCaseId(), is(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId()));
        assertThat(result.get(0).getProsecutionCaseReference(), nullValue());

        assertThat(result.get(1).getCaseStatus(), is(courtApplication.getCourtApplicationCases().get(1).getCaseStatus()));
        assertThat(result.get(1).getProsecutionCaseId(), is(courtApplication.getCourtApplicationCases().get(1).getProsecutionCaseId()));
        assertThat(result.get(1).getProsecutionCaseReference(), is(courtApplication.getCourtApplicationCases().get(1).getProsecutionCaseIdentifier().getCaseURN()));
    }

}