package uk.gov.moj.cpp.progression.service;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import uk.gov.justice.services.core.requester.Requester;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

public class CalendarService {

    public static final String ENGLAND_AND_WALES_DIVISION = "england-and-wales";

    @Inject
    private RefDataService referenceDataService;

    public LocalDate plusWorkingDays(final LocalDate date, final Long numberOfDays, final Requester requester) {
        final List<LocalDate> publicHolidaysList = referenceDataService.getPublicHolidays(ENGLAND_AND_WALES_DIVISION, date, date.plusDays(30), requester);

        LocalDate adjustedLocalDate = date;
        for (int count = 0; count < numberOfDays; count++) {
            adjustedLocalDate = getAdjustedWorkingDate(adjustedLocalDate.plusDays(1), publicHolidaysList);
        }
        return adjustedLocalDate;
    }

    private LocalDate getAdjustedWorkingDate(final LocalDate localDate, final List<LocalDate> publicHolidays) {
        LocalDate adjustedLocalDate = localDate;
        while (isDateOnAWeekend(adjustedLocalDate) || publicHolidays.contains(adjustedLocalDate)) {
            adjustedLocalDate = adjustedLocalDate.plusDays(1);
        }
        return adjustedLocalDate;
    }

    private boolean isDateOnAWeekend(final LocalDate localDate) {
        return localDate.getDayOfWeek() == SUNDAY || localDate.getDayOfWeek() == SATURDAY;
    }
}
