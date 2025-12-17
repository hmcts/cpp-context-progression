package uk.gov.moj.cpp.progression.domain;

import java.util.List;
import java.util.UUID;

public class EmailNotification {

    private final UUID caseId;

    private final UUID materialId;

    private final UUID applicationId;

    private List<Notification> notifications;

    public EmailNotification(final UUID caseId, final UUID materialId, final UUID applicationId, final List<Notification> notifications) {
        this.caseId = caseId;
        this.materialId = materialId;
        this.applicationId = applicationId;
        this.notifications = notifications;
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
