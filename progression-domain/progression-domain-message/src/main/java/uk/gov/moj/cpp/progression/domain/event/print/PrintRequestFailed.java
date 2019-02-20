package uk.gov.moj.cpp.progression.domain.event.print;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.print-request-failed")
public class PrintRequestFailed {

    private final UUID caseId;
    private final UUID notificationId;
    private final ZonedDateTime failedTime;
    private final String errorMessage;
    private final Optional<Integer> statusCode;

    @JsonCreator
    public PrintRequestFailed(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("failedTime") final ZonedDateTime failedTime,
            @JsonProperty("errorMessage") final String errorMessage,
            @JsonProperty("statusCode") final Optional<Integer> statusCode) {
        this.caseId = caseId;
        this.notificationId = notificationId;
        this.failedTime = failedTime;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Optional<Integer> getStatusCode() {
        return statusCode;
    }

    @SuppressWarnings({"squid:S00121", "squid:S00122", "squid:S1067"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final PrintRequestFailed that = (PrintRequestFailed) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(failedTime, that.failedTime) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(statusCode, that.statusCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, notificationId, failedTime, errorMessage, statusCode);
    }

    @Override
    public String toString() {
        return "PrintRequestFailed{" +
                "caseId=" + caseId +
                ", notificationId='" + notificationId +
                ", failedTime='" + failedTime +
                ", errorMessage='" + errorMessage +
                ", statusCode='" + statusCode + "'" +
                '}';
    }
}