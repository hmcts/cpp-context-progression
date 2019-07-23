package uk.gov.moj.cpp.prosecutioncase.persistence.builder;

import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;

import java.time.ZonedDateTime;
import java.util.UUID;

public final class NotificationStatusBuilder {
    private UUID notificationId;
    private UUID caseId;
    private UUID applicationId;
    private UUID materialId;
    private String errorMessage;
    private Integer statusCode;
    private NotificationStatus notificationStatus;
    private NotificationType notificationType;
    private ZonedDateTime updated;
    private String payload;

    private NotificationStatusBuilder() {
    }

    public static NotificationStatusBuilder notificationStatusBuilder() {
        return new NotificationStatusBuilder();
    }

    public NotificationStatusBuilder withCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public NotificationStatusBuilder withApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public NotificationStatusBuilder withNotificationId(final UUID notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public NotificationStatusBuilder withMaterialId(final UUID materialId) {
        this.materialId = materialId;
        return this;
    }

    public NotificationStatusBuilder withErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public NotificationStatusBuilder withStatusCode(final Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public NotificationStatusBuilder withUpdated(final ZonedDateTime updated) {
        this.updated = updated;
        return this;
    }

    public NotificationStatusBuilder withNotificationStatus(final NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
        return this;
    }

    public NotificationStatusBuilder withNotificationType(final NotificationType notificationType) {
        this.notificationType = notificationType;
        return this;
    }

    public NotificationStatusBuilder withPayload(final String payload) {
        this.payload = payload;
        return this;
    }

    public NotificationStatusEntity build() {

        final NotificationStatusEntity notificationStatusEntity = new NotificationStatusEntity();
        notificationStatusEntity.setNotificationId(notificationId);
        notificationStatusEntity.setCaseId(caseId);
        notificationStatusEntity.setApplicationId(applicationId);
        notificationStatusEntity.setMaterialId(materialId);
        notificationStatusEntity.setNotificationStatus(notificationStatus);
        notificationStatusEntity.setNotificationType(notificationType);
        notificationStatusEntity.setPayload(payload);
        notificationStatusEntity.setErrorMessage(errorMessage);
        notificationStatusEntity.setStatusCode(statusCode);
        notificationStatusEntity.setUpdated(updated);
        return notificationStatusEntity;
    }
}
