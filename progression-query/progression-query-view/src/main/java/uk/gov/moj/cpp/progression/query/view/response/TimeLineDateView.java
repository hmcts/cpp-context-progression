package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeLineDateView {

    private String type;

    private LocalDate deadlineDate;

    private long daysFromStartDate;

    private long daysToDeadline;

    private LocalDate startDate;

    public TimeLineDateView(@JsonProperty("type") String type, @JsonProperty("startDate") LocalDate startDate,
            @JsonProperty("daysFromStartDate") long daysFromStartDate,
            @JsonProperty("deadlineDate") LocalDate deadlineDate, @JsonProperty("daysToDeadline") long daysToDeadline) {
        super();
        this.type = type;
        this.deadlineDate = deadlineDate;
        this.daysFromStartDate = daysFromStartDate;
        this.daysToDeadline = daysToDeadline;
        this.startDate = startDate;
    }

    public String getType() {
        return type;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    public long getDaysFromStartDate() {
        return daysFromStartDate;
    }

    public long getDaysToDeadline() {
        return daysToDeadline;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
