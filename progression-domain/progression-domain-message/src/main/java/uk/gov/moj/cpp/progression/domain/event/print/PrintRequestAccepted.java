package uk.gov.moj.cpp.progression.domain.event.print;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.print-request-accepted")
public class PrintRequestAccepted {

    private final UUID caseId;
    private final UUID notificationId;
    private final ZonedDateTime acceptedTime;

    @JsonCreator
    public PrintRequestAccepted(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("acceptedTime") final ZonedDateTime acceptedTime) {
        this.caseId = caseId;
        this.notificationId = notificationId;
        this.acceptedTime = acceptedTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public ZonedDateTime getAcceptedTime() {
        return acceptedTime;
    }

    @Override
    @SuppressWarnings({"squid:S00121", "squid:S00122", "squid:S1067"})
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final PrintRequestAccepted that = (PrintRequestAccepted) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(acceptedTime, that.acceptedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, notificationId, acceptedTime);
    }

    @Override
    public String toString() {
        return "PrintRequestAccepted{" +
                "caseId=" + caseId +
                ", notificationId=" + notificationId +
                ", acceptedTime=" + acceptedTime +
                '}';
    }
}
