package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1948", "squid:S1067", "pmd:BeanMembersShouldSerialize"})
public class NonDefaultDay implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String courtScheduleId;

    private final Integer courtRoomId;

    private final Integer duration;

    private final String oucode;

    private final String session;

    private final ZonedDateTime startTime;

    private final String courtCentreId;

    private final String roomId;

    private final Boolean virtual;

    public NonDefaultDay(final Optional<String> courtScheduleId, final Optional<Integer> courtRoomId, final Optional<Integer> duration, final Optional<String> oucode, final Optional<String> session, final ZonedDateTime startTime, final Optional<String> courtCentreId, final Optional<String> roomId, final Optional<Boolean> virtual) {
        this.courtScheduleId = courtScheduleId.orElse(null);
        this.courtRoomId = courtRoomId.orElse(null);
        this.duration = duration.orElse(null);
        this.oucode = oucode.orElse(null);
        this.session = session.orElse(null);
        this.startTime = startTime;
        this.courtCentreId = courtCentreId.orElse(null);
        this.roomId = roomId.orElse(null);
        this.virtual = virtual.orElse(null);
    }

    public Optional<String> getCourtScheduleId() {
        return courtScheduleId != null ? of(courtScheduleId) : empty();
    }

    public Optional<Integer> getCourtRoomId() {
        return courtRoomId != null ? of(courtRoomId) : empty();
    }

    public Optional<Integer> getDuration() {
        return duration != null ? of(duration) : empty();
    }

    public Optional<String> getOucode() {
        return oucode != null ? of(oucode) : empty();
    }

    public Optional<String> getSession() {
        return session != null ? of(session) : empty();
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public Optional<String> getCourtCentreId() {
        return courtCentreId != null ? of(courtCentreId): empty();
    }

    public Optional<String> getRoomId() {
        return roomId != null ? of(roomId): empty();
    }

    public Optional<Boolean> getVirtual() {
        return virtual != null ? of(virtual) : empty();
    }

    public static Builder nonDefaultDay() {
        return new NonDefaultDay.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NonDefaultDay that = (NonDefaultDay) o;
        return Objects.equals(courtScheduleId, that.courtScheduleId) &&
                Objects.equals(courtRoomId, that.courtRoomId) &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(oucode, that.oucode) &&
                Objects.equals(session, that.session) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(courtCentreId, that.courtCentreId) &&
                Objects.equals(roomId, that.roomId) &&
                Objects.equals(virtual, that.virtual);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtScheduleId, courtRoomId, duration, oucode, session, startTime, courtCentreId, roomId, virtual);
    }

    @Override
    public String toString() {
        return "NonDefaultDay{" +
                "courtScheduleId='" + courtScheduleId + '\'' +
                ", courtRoomId=" + courtRoomId +
                ", duration=" + duration +
                ", oucode='" + oucode + '\'' +
                ", session='" + session + '\'' +
                ", startTime=" + startTime +
                ", courtCentreId='" + courtCentreId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", virtual=" + virtual +
                '}';
    }

    public static class Builder {
        private Optional<String> courtScheduleId = empty();

        private Optional<Integer> courtRoomId = empty();

        private Optional<Integer> duration;

        private Optional<String> oucode = empty();

        private Optional<String> session = empty();

        private ZonedDateTime startTime;

        private Optional<String> courtCentreId = empty();

        private Optional<String> roomId = empty();

        private Optional<Boolean> virtual = empty();

        public Builder withCourtScheduleId(final Optional<String> courtScheduleId) {
            this.courtScheduleId = courtScheduleId;
            return this;
        }

        public Builder withCourtRoomId(final Optional<Integer> courtroomId) {
            this.courtRoomId = courtroomId;
            return this;
        }

        public Builder withDuration(final Optional<Integer> duration) {
            this.duration = duration;
            return this;
        }

        public Builder withOucode(final Optional<String> oucode) {
            this.oucode = oucode;
            return this;
        }

        public Builder withSession(final Optional<String> session) {
            this.session = session;
            return this;
        }

        public Builder withStartTime(final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withCourtCentreId(final Optional<String> courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withRoomId(final Optional<String> roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder withVirtual(final Optional<Boolean> virtual) {
            this.virtual = virtual;
            return this;
        }

        public NonDefaultDay build() {
            return new NonDefaultDay(courtScheduleId, courtRoomId, duration, oucode, session, startTime, courtCentreId, roomId, virtual);
        }

        public NonDefaultDay.Builder withValuesFrom(final NonDefaultDay nonDefaultDay) {
            this.courtCentreId = ofNullable(nonDefaultDay.courtCentreId);
            this.courtRoomId = ofNullable(nonDefaultDay.courtRoomId);
            this.courtScheduleId = ofNullable(nonDefaultDay.courtScheduleId);
            this.duration = ofNullable(nonDefaultDay.duration);
            this.oucode = ofNullable(nonDefaultDay.oucode);
            this.roomId = ofNullable(nonDefaultDay.roomId);
            this.session = ofNullable(nonDefaultDay.session);
            this.startTime = nonDefaultDay.startTime;
            this.virtual = ofNullable(nonDefaultDay.virtual);
            return this;
        }
    }
}
