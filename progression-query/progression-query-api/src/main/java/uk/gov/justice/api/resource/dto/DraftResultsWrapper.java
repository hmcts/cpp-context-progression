package uk.gov.justice.api.resource.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DraftResultsWrapper {

    private UUID hearingId;
    private LocalDate hearingDay;
    private List<ResultLine> resultLines;

    private ZonedDateTime lastSharedTime;

    @JsonCreator
    public DraftResultsWrapper(final UUID hearingId, final LocalDate hearingDay, final List<ResultLine> resultLines, final ZonedDateTime lastSharedTime) {
        this.hearingId = hearingId;
        this.hearingDay = hearingDay;
        this.resultLines = resultLines;
        this.lastSharedTime = lastSharedTime;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public LocalDate getHearingDay() {
        return hearingDay;
    }

    public void setHearingDay(final LocalDate hearingDay) {
        this.hearingDay = hearingDay;
    }

    public List<ResultLine> getResultLines() {
        return resultLines;
    }

    public void setResultLines(final List<ResultLine> resultLines) {
        this.resultLines = resultLines;
    }

    public ZonedDateTime getLastSharedTime() {
        return lastSharedTime;
    }

    public void setLastSharedTime(final ZonedDateTime lastSharedTime) {
        this.lastSharedTime = lastSharedTime;
    }
}
