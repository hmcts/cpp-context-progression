package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;

public class TimeLineDate {

    private TimeLineDateType type;

    private LocalDate startDate;

    private LocalDate referenceDate;

    private long daysFromStartDate;

    public TimeLineDate(TimeLineDateType type, LocalDate startDate, LocalDate referenceDate, long daysFromStartDate) {
        super();
        this.type = type;
        this.startDate = startDate;
        this.referenceDate = referenceDate;
        this.daysFromStartDate = daysFromStartDate;
    }

    public TimeLineDateType getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public long getDaysFromStartDate() {
        return daysFromStartDate;
    }

    public LocalDate getDeadLineDate() {
        return startDate.plusDays(daysFromStartDate);
    }

    public long getDaysToDeadline() {
        return ChronoUnit.DAYS.between(referenceDate, this.getDeadLineDate());
    }

}
