package uk.gov.moj.cpp.progression.domain.event.print;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.print-requested")
public class PrintRequested {

    private final UUID applicationId;
    private final UUID caseId;
    private final UUID notificationId;
    private final UUID materialId;
    private final boolean postage;

    @JsonCreator
    public PrintRequested(
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("postage") final boolean postage) {
        this.applicationId = applicationId;
        this.caseId = caseId;
        this.notificationId = notificationId;
        this.materialId = materialId;
        this.postage = postage;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getApplicationId() { return applicationId; }

    public boolean isPostage() {
        return postage;
    }
}


