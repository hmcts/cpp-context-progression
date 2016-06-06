package uk.gov.moj.cpp.progression.domain.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalDateUtils {

    public static List<LocalDate> getNoOfDatesFromStartDate(final LocalDate startDate, final Integer noOfDays) {
        return IntStream.range(0, noOfDays).mapToObj(action -> startDate.plusDays(action)).collect(Collectors.toList());
    }


    public static Long noOfDaysUntil(final LocalDate endDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }

    private LocalDateUtils() {

    }

}
