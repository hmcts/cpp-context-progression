package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_ACCEPTED;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_FAILED;
import static uk.gov.moj.cpp.progression.domain.constant.PrintStatusType.PRINT_REQUEST_SUCCEEDED;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.PrintStatusType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrintStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrintStatusRepository;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import com.google.common.collect.Maps;

@ServiceComponent(EVENT_LISTENER)
public class PrintListener {

    private static final String CASE_ID = "caseId";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String MATERIAL_ID = "materialId";
    private static final String STATUS_CODE = "statusCode";

    @Inject
    private PrintStatusRepository printStatusRepository;

    @SuppressWarnings("squid:S3655")
    @Transactional
    @Handles("progression.event.print-requested")
    public void printRequested(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));
        final UUID materialId = UUID.fromString(payload.getString(MATERIAL_ID));

        final ZonedDateTime updated = event.metadata().createdAt().get();

        final PrintStatus printStatus = new PrintStatus();
        printStatus.setNotificationId(notificationId);
        printStatus.setMaterialId(materialId);
        printStatus.setCaseId(caseId);
        printStatus.setStatus(PRINT_REQUEST);
        printStatus.setUpdated(updated);

        printStatusRepository.save(printStatus);
    }

    @Transactional
    @Handles("progression.event.print-request-accepted")
    public void printRequestAccepted(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));

        final ZonedDateTime updated = ZonedDateTimes.fromString(payload.getString("acceptedTime"));

        final PrintStatus printStatus = getPrintStatusWith(notificationId, PRINT_REQUEST_ACCEPTED);

        printStatus.setCaseId(caseId);
        printStatus.setStatus(PRINT_REQUEST_ACCEPTED);
        printStatus.setUpdated(updated);

        printStatusRepository.save(printStatus);
    }

    @Transactional
    @Handles("progression.event.print-request-failed")
    public void printRequestFailed(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));
        final ZonedDateTime updated = ZonedDateTimes.fromString(payload.getString("failedTime"));
        final String errorMessage = payload.getString("errorMessage");

        final PrintStatus printStatus = getPrintStatusWith(notificationId, PRINT_REQUEST_FAILED);

        printStatus.setCaseId(caseId);
        printStatus.setStatus(PRINT_REQUEST_FAILED);
        printStatus.setUpdated(updated);
        printStatus.setErrorMessage(errorMessage);

        if (payload.containsKey(STATUS_CODE)) {
            printStatus.setStatusCode((payload.getInt(STATUS_CODE)));
        }

        printStatusRepository.save(printStatus);
    }

    @Transactional
    @Handles("progression.event.print-request-succeeded")
    public void printRequestSucceeded(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final UUID notificationId = UUID.fromString(payload.getString(NOTIFICATION_ID));
        final ZonedDateTime updated = ZonedDateTimes.fromString(payload.getString("sentTime"));

        final PrintStatus printStatus = getPrintStatusWith(notificationId, PRINT_REQUEST_SUCCEEDED);

        printStatus.setCaseId(caseId);
        printStatus.setStatus(PRINT_REQUEST_SUCCEEDED);
        printStatus.setUpdated(updated);

        printStatusRepository.save(printStatus);
    }

    private PrintStatus getPrintStatusWith(final UUID notificationId, final PrintStatusType statusType) {
        final Map<PrintStatusType, PrintStatus> printStatusMap = printStatuses(notificationId);
        final PrintStatus printStatus = new PrintStatus();
        printStatus.setNotificationId(notificationId);
        printStatus.setMaterialId(printStatusMap.get(PRINT_REQUEST).getMaterialId());

        if (isPrintOrderRequestProcessed(statusType)) {
            printStatusMap.get(PRINT_REQUEST_ACCEPTED);
        }
        return printStatus;
    }

    private boolean isPrintOrderRequestProcessed(final PrintStatusType statusType) {
        return statusType == PRINT_REQUEST_SUCCEEDED || statusType == PRINT_REQUEST_FAILED;
    }

    private Map<PrintStatusType, PrintStatus> printStatuses(final UUID notificationId) {
        return Maps.uniqueIndex(printStatusRepository.findByNotificationId(notificationId), PrintStatus::getStatus);
    }

}