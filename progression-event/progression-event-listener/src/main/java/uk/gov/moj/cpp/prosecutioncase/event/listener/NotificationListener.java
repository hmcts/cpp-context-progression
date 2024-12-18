package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_ACCEPTED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_NOT_SENT;
import static uk.gov.moj.cpp.progression.domain.constant.NotificationStatus.NOTIFICATION_REQUEST_SUCCEEDED;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class NotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class.getName());
    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String MATERIAL_ID = "materialId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String STATUS_CODE = "statusCode";
    private static final String PAYLOAD = "payload";
    private static final String FAILED_TIME = "failedTime";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SENT_TIME = "sentTime";
    private static final String ACCEPTED_TIME = "acceptedTime";
    private static final String NOTIFICATIONS = "notifications";

    @Inject
    private NotificationStatusRepository notificationStatusRepository;

    @SuppressWarnings("squid:S3655")
    @Transactional
    @Handles("progression.event.print-requested")
    public void printRequested(final JsonEnvelope event) {

        LOGGER.info("Received progression.event.print-requested event {}", event);

        final JsonObject payload = event.payloadAsJsonObject();

        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));

        final UUID materialId = UUID.fromString(payload.getString(MATERIAL_ID));

        final ZonedDateTime updated = event.metadata().createdAt().get();

        final NotificationStatusEntity notificationStatus = new NotificationStatusEntity();
        notificationStatus.setNotificationId(notificationId);
        notificationStatus.setMaterialId(materialId);
        notificationStatus.setNotificationStatus(NOTIFICATION_REQUEST);
        notificationStatus.setNotificationType(NotificationType.PRINT);
        notificationStatus.setUpdated(updated);

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
            notificationStatus.setCaseId(caseId);
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = UUID.fromString(payload.getString(APPLICATION_ID));
            notificationStatus.setCaseId(applicationId);
        }

        notificationStatusRepository.save(notificationStatus);
    }

    @Transactional
    @Handles("progression.event.email-requested")
    public void emailRequested(final JsonEnvelope event) {

        LOGGER.info("Received progression.event.email-requested event {}", event);

        final JsonObject jsonObject = event.payloadAsJsonObject();
        final ZonedDateTime updated = event.metadata().createdAt().orElse(ZonedDateTime.now());
        final JsonArray notifications = jsonObject.getJsonArray(NOTIFICATIONS);

        for (final JsonValue emailPayload : notifications) {

            final JsonObject payload = (JsonObject) emailPayload;

            createNotificationStatus(jsonObject, payload, NOTIFICATION_REQUEST, updated);
        }
    }

    @Transactional
    @Handles("progression.event.notification-request-accepted")
    public void notificationRequestAccepted(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();

        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));

        final NotificationStatusEntity notificationStatus = getPrintStatusWith(notificationId, NOTIFICATION_REQUEST_ACCEPTED);

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
            notificationStatus.setCaseId(caseId);
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = UUID.fromString(payload.getString(APPLICATION_ID));
            notificationStatus.setApplicationId(applicationId);
        }

        if (payload.containsKey(MATERIAL_ID)) {
            final UUID materialId = UUID.fromString(payload.getString(MATERIAL_ID));
            notificationStatus.setMaterialId(materialId);
        }

        if (payload.containsKey(ACCEPTED_TIME)) {
            final ZonedDateTime updated = ZonedDateTimes.fromString(payload.getString(ACCEPTED_TIME));
            notificationStatus.setUpdated(updated);
        }

        notificationStatus.setNotificationStatus(NOTIFICATION_REQUEST_ACCEPTED);
        notificationStatusRepository.save(notificationStatus);
    }

    @Transactional
    @Handles("progression.event.email-request-not-sent")
    public void emailRequestNotSent(final JsonEnvelope event) {

        LOGGER.info("Received progression.event.email-request-not-sent event {}", event);

        final JsonObject jsonObject = event.payloadAsJsonObject();
        final ZonedDateTime updated = event.metadata().createdAt().orElse(ZonedDateTime.now());

        final JsonObject payload = jsonObject.getJsonObject("notification");

        createNotificationStatus(jsonObject, payload, NOTIFICATION_REQUEST_NOT_SENT, updated);
    }

    @Transactional
    @Handles("progression.event.notification-request-failed")
    public void printRequestFailed(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));
        final ZonedDateTime updated = ZonedDateTimes.fromString(payload.getString(FAILED_TIME));
        final String errorMessage = payload.getString(ERROR_MESSAGE);

        final NotificationStatusEntity notificationStatus = getPrintStatusWith(notificationId, NOTIFICATION_REQUEST_FAILED);

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
            notificationStatus.setCaseId(caseId);
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = UUID.fromString(payload.getString(APPLICATION_ID));
            notificationStatus.setApplicationId(applicationId);
        }

        if (payload.containsKey(MATERIAL_ID)) {
            final UUID materialId = UUID.fromString(payload.getString(MATERIAL_ID));
            notificationStatus.setMaterialId(materialId);
        }

        notificationStatus.setNotificationStatus(NOTIFICATION_REQUEST_FAILED);
        notificationStatus.setUpdated(updated);
        notificationStatus.setErrorMessage(errorMessage);

        if (payload.containsKey(STATUS_CODE)) {
            notificationStatus.setStatusCode((payload.getInt(STATUS_CODE)));
        }

        notificationStatusRepository.save(notificationStatus);
    }

    @Transactional
    @Handles("progression.event.notification-request-succeeded")
    public void printRequestSucceeded(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));
        final ZonedDateTime updated = ZonedDateTimes.fromString(payload.getString(SENT_TIME));

        final NotificationStatusEntity notificationStatus = getPrintStatusWith(notificationId, NOTIFICATION_REQUEST_SUCCEEDED);

        if (payload.containsKey(CASE_ID)) {
            final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
            notificationStatus.setCaseId(caseId);
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final UUID applicationId = UUID.fromString(payload.getString(APPLICATION_ID));
            notificationStatus.setApplicationId(applicationId);
        }

        if (payload.containsKey(MATERIAL_ID)) {
            final UUID materialId = UUID.fromString(payload.getString(MATERIAL_ID));
            notificationStatus.setMaterialId(materialId);
        }

        notificationStatus.setNotificationStatus(NOTIFICATION_REQUEST_SUCCEEDED);
        notificationStatus.setUpdated(updated);
        notificationStatusRepository.save(notificationStatus);
    }

    private NotificationStatusEntity getPrintStatusWith(final UUID notificationId, final NotificationStatus status) {
        final Map<NotificationStatus, NotificationStatusEntity> notificationStatusMap = printStatuses(notificationId);
        final NotificationStatusEntity notificationStatus = new NotificationStatusEntity();
        notificationStatus.setNotificationId(notificationId);
        notificationStatus.setMaterialId(notificationStatusMap.get(NOTIFICATION_REQUEST).getMaterialId());
        notificationStatus.setNotificationType(notificationStatusMap.get(NOTIFICATION_REQUEST).getNotificationType());
        if (isPrintOrderRequestProcessed(status)) {
            notificationStatusMap.get(NOTIFICATION_REQUEST_ACCEPTED);
        }
        return notificationStatus;
    }

    private boolean isPrintOrderRequestProcessed(final NotificationStatus status) {
        return status == NOTIFICATION_REQUEST_SUCCEEDED || status == NOTIFICATION_REQUEST_FAILED;
    }

    private Map<NotificationStatus, NotificationStatusEntity> printStatuses(final UUID notificationId) {
        final List<NotificationStatusEntity> notificationStatusEntityList = notificationStatusRepository.findByNotificationId(notificationId);
        return notificationStatusEntityList.stream()
                .collect(Collectors.toMap(
                        NotificationStatusEntity::getNotificationStatus,
                        notificationStatus -> notificationStatus,
                        (oldValue, newValue) -> oldValue));
    }

    private void createNotificationStatus(final JsonObject jsonObject, final JsonObject payload, final NotificationStatus notificationRequest, final ZonedDateTime updated) {
        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));

        final NotificationStatusEntity notificationStatus = new NotificationStatusEntity();
        notificationStatus.setNotificationId(notificationId);
        notificationStatus.setNotificationStatus(notificationRequest);
        notificationStatus.setNotificationType(NotificationType.EMAIL);
        notificationStatus.setUpdated(updated);

        if (jsonObject.containsKey(APPLICATION_ID)) {
            final UUID applicationId = UUID.fromString(jsonObject.getString(APPLICATION_ID));
            notificationStatus.setApplicationId(applicationId);
        }

        if (jsonObject.containsKey(CASE_ID)) {
            final UUID caseId = UUID.fromString(jsonObject.getString(CASE_ID));
            notificationStatus.setCaseId(caseId);
        }

        if (jsonObject.containsKey(MATERIAL_ID)) {
            final UUID materialId = UUID.fromString(jsonObject.getString(MATERIAL_ID));
            notificationStatus.setMaterialId(materialId);
        }

        if (payload.containsKey(PAYLOAD)) {
            final String payloadString = payload.getString(PAYLOAD);
            notificationStatus.setPayload(payloadString);
        }

        notificationStatusRepository.save(notificationStatus);
    }

}
