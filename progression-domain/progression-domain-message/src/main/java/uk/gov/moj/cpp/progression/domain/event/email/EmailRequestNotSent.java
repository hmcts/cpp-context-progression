package uk.gov.moj.cpp.progression.domain.event.email;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.Notification;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.email-request-not-sent")
public class EmailRequestNotSent {

    private final UUID caseId;
    private final UUID materialId;
    private final UUID applicationId;

    private final Notification notification;

    @JsonCreator
    public EmailRequestNotSent(@JsonProperty("caseId") final UUID caseId,
                               @JsonProperty("materialId") final UUID materialId,
                               @JsonProperty("applicationId") final UUID applicationId,
                               @JsonProperty("notification") final Notification notification) {
        this.caseId = caseId;
        this.notification = notification;
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

    public Notification getNotification() {
        return notification;
    }
}


