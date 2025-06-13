package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.query.laa.HearingSummary;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingSummaryLaaConverterTest {

    @InjectMocks
    private HearingSummaryLaaConverter hearingSummaryLaaConverter;

    @Mock
    private CourtCentreConverter courtCentreConverter;

    @Mock
    private HearingDayConverter hearingDayConverter;

    @Mock
    private HearingTypeConverter hearingTypeConverter;

    @Mock
    private DefenceCounselConverter defenceCounselConverter;

    @Test
    void shouldReturnNullWhenHearingIsNull() {
        final List<HearingSummary> result = hearingSummaryLaaConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenHearingIsEmpty() {
        final List<HearingSummary> result = hearingSummaryLaaConverter.convert(emptyList());

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertHearingToHearingSummary() {
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(asList(
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .build()
                                ))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withDefendants(asList(
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .build()
                                ))
                                .build()))
                .withEstimatedDuration("10")
                .withJurisdictionType(CROWN)
                .build();

        final List<HearingSummary> result = hearingSummaryLaaConverter.convert(singletonList(hearing));
        assertThat(result.get(0).getHearingId(), is(hearing.getId()));
        assertThat(result.get(0).getDefendantIds().size(), is(4));
        assertThat(result.get(0).getEstimatedDuration(), is(hearing.getEstimatedDuration()));
        assertThat(result.get(0).getJurisdictionType().toString(), is(hearing.getJurisdictionType().toString()));
    }

    @Test
    void shouldConvertHearingToHearingSummaryWhenThereAreNullValues() {
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withEstimatedDuration("10")
                .build();

        final List<HearingSummary> result = hearingSummaryLaaConverter.convert(singletonList(hearing));
        assertThat(result.get(0).getHearingId(), is(hearing.getId()));
        assertThat(result.get(0).getDefendantIds(), nullValue());
        assertThat(result.get(0).getEstimatedDuration(), is(hearing.getEstimatedDuration()));
        assertThat(result.get(0).getJurisdictionType(), nullValue());
    }

}