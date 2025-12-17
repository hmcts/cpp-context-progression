package uk.gov.moj.cpp.progression.domain.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalDateUtils {

    private LocalDateUtils() {

    }

    public static List<LocalDate> getNoOfDatesFromStartDate(final LocalDate startDate,
                    final Integer noOfDays) {
        return IntStream.range(0, noOfDays).mapToObj(action -> startDate.plusDays(action))
                        .collect(Collectors.toList());
    }


    public static Long noOfDaysUntil(final LocalDate endDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }



    public static LocalDate addWorkingDays(final LocalDate startDate, int noOfDays) {

        if (noOfDays < 1 || startDate == null) {
            return startDate;
        }

        return startDate.plusDays(
                        getActualNumberOfDaysToAdd(noOfDays, startDate.getDayOfWeek().getValue()));
    }

    private static long getActualNumberOfDaysToAdd(long workdays, int dayOfWeek) {
        if (dayOfWeek < 6) { // date is a workday
            return workdays + (workdays + dayOfWeek - 1) / 5 * 2;
        } else { // date is a weekend
            return workdays + (workdays - 1) / 5 * 2 + (7 - dayOfWeek);
        }
    }

    public static Boolean isYouth(final LocalDate defendantDob, final LocalDate firstHearingDate) {
        final Period p = Period.between(defendantDob, firstHearingDate);
        return p.getYears()<18;
    }
}
