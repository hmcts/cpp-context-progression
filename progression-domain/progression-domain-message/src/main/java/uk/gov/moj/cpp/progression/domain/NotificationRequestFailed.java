package uk.gov.moj.cpp.progression.domain;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.notification-request-failed")
public class NotificationRequestFailed {

    private final UUID caseId;
    private final UUID notificationId;
    private final UUID applicationId;
    private final UUID materialId;
    private final ZonedDateTime failedTime;
    private final String errorMessage;
    private final Optional<Integer> statusCode;

    @JsonCreator
    public NotificationRequestFailed(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("failedTime") final ZonedDateTime failedTime,
            @JsonProperty("errorMessage") final String errorMessage,
            @JsonProperty("statusCode") final Optional<Integer> statusCode) {
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.materialId = materialId;
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

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    @SuppressWarnings({"squid:S00121", "squid:S00122", "squid:S1067"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NotificationRequestFailed that = (NotificationRequestFailed) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(materialId, that.materialId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(failedTime, that.failedTime) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(statusCode, that.statusCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, applicationId, materialId, notificationId, failedTime, errorMessage, statusCode);
    }

    @Override
    public String toString() {
        return "PrintRequestFailed{" +
                "caseId=" + caseId +
                "applicationId=" + applicationId +
                ", materialId='" + materialId +
                ", notificationId='" + notificationId +
                ", failedTime='" + failedTime +
                ", errorMessage='" + errorMessage +
                ", statusCode='" + statusCode + "'" +
                '}';
    }
}