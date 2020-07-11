package uk.gov.moj.cpp.progression.domain;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.notification-request-accepted")
public class NotificationRequestAccepted {

    private final UUID caseId;
    private final UUID applicationId;
    private final UUID materialId;
    private final UUID notificationId;
    private final ZonedDateTime acceptedTime;

    @JsonCreator
    public NotificationRequestAccepted(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("acceptedTime") final ZonedDateTime acceptedTime) {
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.materialId = materialId;
        this.notificationId = notificationId;
        this.acceptedTime = acceptedTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public ZonedDateTime getAcceptedTime() {
        return acceptedTime;
    }

    public UUID getMaterialId() {
        return materialId;
    }
}
