package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067", "squid:S1948"})
public class HearingDay implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Optional<UUID> courtScheduleId;

    private final Integer durationMinutes;

    private final ZonedDateTime endTime;

    private final LocalDate hearingDate;

    private final Integer sequence;

    private final ZonedDateTime startTime;

    private final Optional<Boolean> isCancelled;

    private final Optional<UUID> courtCentreId;

    private final Optional<UUID> courtRoomId;

    public HearingDay(final Integer durationMinutes, final ZonedDateTime endTime, final LocalDate hearingDate, final Integer sequence, final ZonedDateTime startTime, final Optional<UUID> courtScheduleId, final Optional<Boolean> isCancelled, final Optional<UUID> courtCentreId, final Optional<UUID> courtRoomId) {
        this.durationMinutes = durationMinutes;
        this.endTime = endTime;
        this.hearingDate = hearingDate;
        this.sequence = sequence;
        this.startTime = startTime;
        this.courtScheduleId = courtScheduleId;
        this.isCancelled = isCancelled;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public Integer getSequence() {
        return sequence;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public Optional<UUID> getCourtScheduleId() {
        return courtScheduleId;
    }

    public static Builder hearingDay() {
        return new HearingDay.Builder();
    }

    public Optional<Boolean> getIsCancelled() {
        return isCancelled;
    }

    public Optional<UUID> getCourtCentreId() {
        return courtCentreId;
    }

    public Optional<UUID> getCourtRoomId() {
        return courtRoomId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HearingDay that = (HearingDay) o;
        return Objects.equals(getCourtScheduleId(), that.getCourtScheduleId()) &&
                Objects.equals(getDurationMinutes(), that.getDurationMinutes()) &&
                Objects.equals(getEndTime(), that.getEndTime()) &&
                Objects.equals(getHearingDate(), that.getHearingDate()) &&
                Objects.equals(getSequence(), that.getSequence()) &&
                Objects.equals(getStartTime(), that.getStartTime()) &&
                Objects.equals(getIsCancelled(), that.getIsCancelled()) &&
                Objects.equals(getCourtCentreId(), that.getCourtCentreId()) &&
                Objects.equals(getCourtRoomId(), that.getCourtRoomId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCourtScheduleId(), getDurationMinutes(), getEndTime(), getHearingDate(), getSequence(), getStartTime(), getIsCancelled(), getCourtCentreId(), getCourtRoomId());
    }

    @Override
    public String toString() {
        return "HearingDay{" +
                "courtScheduleId=" + courtScheduleId +
                ", durationMinutes=" + durationMinutes +
                ", endTime=" + endTime +
                ", hearingDate=" + hearingDate +
                ", sequence=" + sequence +
                ", startTime=" + startTime +
                ", isCancelled=" + isCancelled +
                ", courtCentreId=" + courtCentreId +
                ", courtRoomId=" + courtRoomId +
                '}';
    }

    public static class Builder {
        private Integer durationMinutes;

        private ZonedDateTime endTime;

        private LocalDate hearingDate;

        private Integer sequence;

        private ZonedDateTime startTime;

        private Optional<UUID> courtScheduleId = empty();

        private Optional<Boolean> isCancelled = empty();

        private Optional<UUID> courtCentreId = empty();

        private Optional<UUID> courtRoomId = empty();

        public Builder withDurationMinutes(final Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder withEndTime(final ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withHearingDate(final LocalDate hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withSequence(final Integer sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder withStartTime(final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withCourtScheduleId(final Optional<UUID> courtScheduleId) {
            this.courtScheduleId = courtScheduleId;
            return this;
        }

        public Builder withIsCancelled(final Optional<Boolean> isCancelled) {
            this.isCancelled = isCancelled;
            return this;
        }

        public Builder withCourtCentreId(final Optional<UUID> courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withCourtRoomId(final Optional<UUID> courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public HearingDay build() {
            return new HearingDay(durationMinutes, endTime, hearingDate, sequence, startTime, courtScheduleId, isCancelled, courtCentreId, courtRoomId);
        }
    }
}
