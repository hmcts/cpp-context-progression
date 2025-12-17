package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.progression.query.laa.HearingType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingTypeConverterTest {

    @InjectMocks
    private HearingTypeConverter hearingTypeConverter;

    @Test
    void shouldReturnNullWhenHearingTypeIsNull() {
        final HearingType result = hearingTypeConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertHearingType() {
        final uk.gov.justice.core.courts.HearingType hearingType = uk.gov.justice.core.courts.HearingType.hearingType()
                .withId(randomUUID())
                .withDescription("description")
                .build();

        final HearingType result = hearingTypeConverter.convert(hearingType);

        assertThat(result.getId(), is(hearingType.getId()));
        assertThat(result.getDescription(), is(hearingType.getDescription()));
    }

}