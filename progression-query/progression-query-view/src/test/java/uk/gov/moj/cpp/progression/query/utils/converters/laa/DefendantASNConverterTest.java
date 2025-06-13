package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtApplicationParty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefendantASNConverterTest {

    public static final String ASN = "ASN";
    @InjectMocks
    private DefendantASNConverter defendantASNConverter;

    @Test
    void shouldReturnNullWhenMasterDefendantIsNull() {
        final CourtApplicationParty subject = CourtApplicationParty.courtApplicationParty().build();
        final String result = defendantASNConverter.convert(subject);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenMasterPersonDefendantIsNull() {
        final CourtApplicationParty subject = CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(uk.gov.justice.core.courts.MasterDefendant.masterDefendant().build())
                .build();
        final String result = defendantASNConverter.convert(subject);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertDefendantASN() {
        final CourtApplicationParty subject = CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(uk.gov.justice.core.courts.MasterDefendant.masterDefendant()
                        .withPersonDefendant(uk.gov.justice.core.courts.PersonDefendant.personDefendant()
                                .withArrestSummonsNumber(ASN)
                                .build())
                        .build())
                .build();
        final String result = defendantASNConverter.convert(subject);

        assertThat(result, is(ASN));
    }


}