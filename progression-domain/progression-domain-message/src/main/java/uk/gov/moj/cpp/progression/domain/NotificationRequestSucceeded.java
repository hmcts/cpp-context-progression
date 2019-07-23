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
    private final UUID notificationId;
    private final ZonedDateTime sentTime;

    @JsonCreator
    public NotificationRequestSucceeded(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("sentTime") final ZonedDateTime sentTime) {
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.notificationId = notificationId;
        this.sentTime = sentTime;
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

    public UUID getApplicationId() {
        return applicationId;
    }

    @SuppressWarnings({"squid:S00121", "squid:S00122",})
    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final NotificationRequestSucceeded that = (NotificationRequestSucceeded) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(sentTime, that.sentTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, notificationId, sentTime);
    }

    @Override
    public String toString() {
        return "PrintRequestSucceeded{" +
                "caseId=" + caseId +
                ", notificationId='" + notificationId +
                ", sentTime='" + sentTime + "'" +
                '}';
    }
}