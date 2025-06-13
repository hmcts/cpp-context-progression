package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.progression.query.laa.CourtCentre;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtCentreConverterTest {

    @InjectMocks
    private CourtCentreConverter courtCentreConverter;

    @Mock
    private AddressConverter addressConverter;

    @Test
    void shouldReturnNullWhenCourtCentreIsNull() {
        final CourtCentre result = courtCentreConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertCourtCentre() {
        final uk.gov.justice.core.courts.CourtCentre courtCentre = uk.gov.justice.core.courts.CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("name")
                .withCode("code")
                .withRoomId(randomUUID())
                .withRoomName("room name")
                .withWelshName("welsh name")
                .withWelshRoomName("welsh room name")
                .build();

        final CourtCentre result = courtCentreConverter.convert(courtCentre);

        assertThat(result.getId(), is(courtCentre.getId()));
        assertThat(result.getName(), is(courtCentre.getName()));
        assertThat(result.getCode(), is(courtCentre.getCode()));
        assertThat(result.getRoomId(), is(courtCentre.getRoomId()));
        assertThat(result.getRoomName(), is(courtCentre.getRoomName()));
        assertThat(result.getWelshName(), is(courtCentre.getWelshName()));
        assertThat(result.getWelshRoomName(), is(courtCentre.getWelshRoomName()));
    }

}