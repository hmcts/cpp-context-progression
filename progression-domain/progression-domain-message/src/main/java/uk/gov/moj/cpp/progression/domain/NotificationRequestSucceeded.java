package uk.gov.moj.cpp.progression.domain;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.notification-request-succeeded")
public class NotificationRequestSucceeded {

    private final UUID caseId;
    private final UUID applicationId;
    private final UUID materialId;
    private final UUID notificationId;
    private final ZonedDateTime sentTime;
    private final ZonedDateTime completedAt;

    @JsonCreator
    public NotificationRequestSucceeded(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("sentTime") final ZonedDateTime sentTime,
            @JsonProperty("completedAt") final ZonedDateTime completedAt
    ) {
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.materialId = materialId;
        this.notificationId = notificationId;
        this.sentTime = sentTime;
        this.completedAt = completedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public ZonedDateTime getSentTime() {
        return sentTime;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
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
        final NotificationRequestSucceeded that = (NotificationRequestSucceeded) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(materialId, that.materialId) &&
                Objects.equals(sentTime, that.sentTime) &&
                Objects.equals(completedAt, that.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, applicationId, notificationId, materialId, sentTime, completedAt);
    }

    @Override
    public String toString() {
        return "PrintRequestSucceeded{" +
                "caseId=" + caseId +
                ", notificationId='" + notificationId +
                ", applicationId='" + applicationId +
                ", materialId='" + materialId +
                ", sentTime='" + sentTime + "'" +
                ", completedAt='" + completedAt + "'" +
                '}';
    }

}