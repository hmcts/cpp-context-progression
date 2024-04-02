package uk.gov.moj.cpp.progression.service;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CalendarServiceTest {

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @InjectMocks
    private CalendarService calendarService;

    @Before
    public void setUp() {
        when(referenceDataService.getPublicHolidays(anyString(), anyObject(), anyObject(), anyObject())).thenReturn(getPublicHolidays());
    }


    @Test
    public void shouldPlusThreeDaysFromNov1Of2023WhenThereIsPublicHolidaysAndWeekEnd() {
        final LocalDate result = calendarService.plusWorkingDays(LocalDate.parse("2023-11-01"), 3L, requester);
        assertThat(result, is(LocalDate.parse("2023-11-08")));
    }

    @Test
    public void shouldPlusFourDaysFromOctober23Of2023WithOutAnyHolidaysOrWeekends() {
        final LocalDate result = calendarService.plusWorkingDays(LocalDate.parse("2023-10-23"), 4L, requester);
        assertThat(result, is(LocalDate.parse("2023-10-27")));
    }

    private List<LocalDate> getPublicHolidays() {
        final List<LocalDate> publicHolidays = new ArrayList();
        publicHolidays.add(LocalDate.parse("2023-11-03"));
        publicHolidays.add(LocalDate.parse("2023-11-06"));
        return publicHolidays;
    }

}