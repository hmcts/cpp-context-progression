package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.progression.query.laa.DefenceCounsel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefenceCounselConverterTest {

    public static final String FIRST_NAME = "first name";
    public static final String MIDDLE_NAME = "middle name";
    public static final String LAST_NAME = "last name";
    public static final String STATUS = "status";
    public static final String TITLE = "title";
    @InjectMocks
    private DefenceCounselConverter defenceCounselConverter;

    @Test
    void shouldReturnNullWhenDefenceCounselsIsNull() {
        final List<DefenceCounsel> result = defenceCounselConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldReturnNullWhenDefenceCounselsIsEmpty() {
        final List<DefenceCounsel> result = defenceCounselConverter.convert(emptyList());

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertDefenceCounsel() {

        final uk.gov.justice.core.courts.DefenceCounsel defenceCounsel1 = createDefenceCounsel(randomUUID());
        final uk.gov.justice.core.courts.DefenceCounsel defenceCounsel2 = createDefenceCounsel(randomUUID());

        final List<DefenceCounsel> result = defenceCounselConverter.convert(asList(defenceCounsel1, defenceCounsel2));

        assertThat(result.size(), is(2));

        assertThat(result.get(0).getId(), is(defenceCounsel1.getId()));
        assertThat(result.get(0).getAttendanceDays().get(0), is(defenceCounsel1.getAttendanceDays().get(0).toString()));
        assertThat(result.get(0).getAttendanceDays().get(1), is(defenceCounsel1.getAttendanceDays().get(1).toString()));
        assertThat(result.get(0).getDefendants(), is(defenceCounsel1.getDefendants()));
        assertThat(result.get(0).getFirstName(), is(defenceCounsel1.getFirstName()));
        assertThat(result.get(0).getMiddleName(), is(defenceCounsel1.getMiddleName()));
        assertThat(result.get(0).getLastName(), is(defenceCounsel1.getLastName()));
        assertThat(result.get(0).getStatus(), is(defenceCounsel1.getStatus()));
        assertThat(result.get(0).getTitle(), is(defenceCounsel1.getTitle()));

    }

    @Test
    void shouldConvertDefenceCounselWhenThereAreNullValues() {

        final uk.gov.justice.core.courts.DefenceCounsel courtCentre = uk.gov.justice.core.courts.DefenceCounsel.defenceCounsel()
                .withId(randomUUID())
                .withDefendants(singletonList(randomUUID()))
                .withFirstName(FIRST_NAME)
                .withMiddleName(MIDDLE_NAME)
                .withLastName(LAST_NAME)
                .withStatus(STATUS)
                .withTitle(TITLE)
                .build();

        final List<DefenceCounsel> result = defenceCounselConverter.convert(singletonList(courtCentre));

        assertThat(result.size(), is(1));

        assertThat(result.get(0).getId(), is(courtCentre.getId()));
        assertThat(result.get(0).getAttendanceDays(), nullValue());
        assertThat(result.get(0).getDefendants(), is(courtCentre.getDefendants()));
        assertThat(result.get(0).getFirstName(), is(courtCentre.getFirstName()));
        assertThat(result.get(0).getMiddleName(), is(courtCentre.getMiddleName()));
        assertThat(result.get(0).getLastName(), is(courtCentre.getLastName()));
        assertThat(result.get(0).getStatus(), is(courtCentre.getStatus()));
        assertThat(result.get(0).getTitle(), is(courtCentre.getTitle()));

    }

    private static uk.gov.justice.core.courts.DefenceCounsel createDefenceCounsel(final UUID id) {
        final uk.gov.justice.core.courts.DefenceCounsel courtCentre = uk.gov.justice.core.courts.DefenceCounsel.defenceCounsel()
                .withId(id)
                .withAttendanceDays(asList(LocalDate.now(), LocalDate.now().plusDays(2)))
                .withDefendants(singletonList(randomUUID()))
                .withFirstName(FIRST_NAME)
                .withMiddleName(MIDDLE_NAME)
                .withLastName(LAST_NAME)
                .withStatus(STATUS)
                .withTitle(TITLE)
                .build();
        return courtCentre;
    }

}