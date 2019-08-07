package uk.gov.moj.cpp.progression.domain.event.email;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.Notification;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.email-requested")
public class EmailRequested {

    private UUID caseId;
    private UUID materialId;
    private UUID applicationId;

    private final List<Notification> notifications;

    @JsonCreator
    public EmailRequested(@JsonProperty("caseId") final UUID caseId,
                          @JsonProperty("materialId") final UUID materialId,
                          @JsonProperty("applicationId") final UUID applicationId,
                          @JsonProperty("notifications") final List<Notification> notifications) {
        this.caseId = caseId;
        this.notifications = notifications;
        this.materialId = materialId;
        this.applicationId = applicationId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }
}


