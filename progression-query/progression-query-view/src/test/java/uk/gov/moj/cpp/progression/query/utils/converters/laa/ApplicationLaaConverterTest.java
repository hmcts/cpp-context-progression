package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.justice.core.courts.ApplicationStatus.IN_PROGRESS;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.progression.query.laa.ApplicationLaa;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationLaaConverterTest {

    @InjectMocks
    private ApplicationLaaConverter applicationLaaConverter;
    @Mock
    private CaseSummaryLaaConverter caseSummaryLaaConverter;
    @Mock
    private HearingSummaryLaaConverter hearingSummaryLaaConverter;
    @Mock
    private SubjectSummaryLaaConverter subjectSummaryLaaConverter;
    @Mock
    private JudicialResultsConverter judicialResultsConverter;

    private static final String LAA_APPLICATION_SHORTID = "A23ABCDEFGH";

    @Test
    void shouldConvertAddress() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withApplicationReference("APP-1234")
                .withApplicationStatus(IN_PROGRESS)
                .withType(CourtApplicationType.courtApplicationType().withType("application type").build())
                .withApplicationReceivedDate(LocalDate.now())
                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                        .withCategory(FINAL)
                        .build()))
                .build();

        ApplicationLaa result = applicationLaaConverter.convert(courtApplication, null, LAA_APPLICATION_SHORTID);

        assertThat(result, notNullValue());
        assertThat(result.getApplicationId(), is(courtApplication.getId()));
        assertThat(result.getApplicationReference(), is(courtApplication.getApplicationReference()));
        assertThat(result.getApplicationStatus(), is(courtApplication.getApplicationStatus().toString()));
        assertThat(result.getApplicationTitle(), is(courtApplication.getType().getType()));
        assertThat(result.getApplicationType(), is(courtApplication.getType().getCode()));
        assertThat(result.getReceivedDate(), is(courtApplication.getApplicationReceivedDate().toString()));
    }

    @Test
    void shouldConvertAddressWithNullValues() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withApplicationReference("APP-1234")
                .withApplicationStatus(IN_PROGRESS)
                .withType(CourtApplicationType.courtApplicationType().withType("application type").build())
                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                        .withCategory(FINAL)
                        .build()))
                .build();

        ApplicationLaa result = applicationLaaConverter.convert(courtApplication, null, LAA_APPLICATION_SHORTID);

        assertThat(result, notNullValue());
        assertThat(result.getApplicationId(), is(courtApplication.getId()));
        assertThat(result.getApplicationReference(), is(courtApplication.getApplicationReference()));
        assertThat(result.getApplicationStatus(), is(courtApplication.getApplicationStatus().toString()));
        assertThat(result.getApplicationTitle(), is(courtApplication.getType().getType()));
        assertThat(result.getApplicationType(), is(courtApplication.getType().getCode()));
        assertThat(result.getReceivedDate(), nullValue());
    }

}