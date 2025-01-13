package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime.updateCourtDocumentPrintTime;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.APPLICATION;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.CASE;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.MATERIAL;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository.NotificationInfoJdbcRepository;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository.NotificationInfoResult;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;

// squid:S1312 - logger not being static final
// squid:S2629 - logger not being called conditionally
@SuppressWarnings({"WeakerAccess", "squid:S2629", "squid:S1312"})
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationNotifyEventProcessor {

    private static final String NOTIFICATION_ID = "notificationId";
    private static final String NOTIFICATION_TYPE = "notificationType";
    private static final String RECIPIENT_TYPE = "recipientType";
    private static final String MATERIAL_ID = "materialId";
    private static final String COMPLETED_AT = "completedAt";
    private static final String EMAIL_DOCUMENT_TEMPLATE_NAME = "HearingEmailNotification";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS");

    @Inject
    private NotificationService notificationService;

    @Inject
    private HearingNotificationHelper hearingNotificationHelper;

    @Inject
    DocumentGeneratorService documentGeneratorService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private NotificationInfoJdbcRepository notificationInfoJdbcRepository;

    @Inject
    private FileStorer fileStorer;

    @Inject
    private Logger logger;

    @Inject
    private Sender sender;

    @Handles("public.notificationnotify.events.notification-failed")
    public void markNotificationAsFailed(final JsonEnvelope event) {

        final UUID notificationId = fromString(event.payloadAsJsonObject().getString(NOTIFICATION_ID));

        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString());

        if (systemIdMapping.isPresent()) {

            notificationService.recordNotificationRequestFailure(event, systemIdMapping.get().getTargetId(), CASE);

        } else {

            final Optional<SystemIdMapping> applicationSystemIdMapping = systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString());

            if (applicationSystemIdMapping.isPresent()) {

                notificationService.recordNotificationRequestFailure(event, applicationSystemIdMapping.get().getTargetId(), APPLICATION);

            } else {

                final Optional<SystemIdMapping> materialSystemIdMapping = systemIdMapperService.getCppMaterialIdForNotificationId(notificationId.toString());

                if (materialSystemIdMapping.isPresent()) {

                    notificationService.recordNotificationRequestFailure(event, materialSystemIdMapping.get().getTargetId(), MATERIAL);

                } else {
                    logger.info(format("No Case, Application or Material found for the given notification id: %s", notificationId));
                }
            }
        }
    }

    @Handles("public.notificationnotify.events.notification-sent")
    public void markNotificationAsSucceeded(final JsonEnvelope event) {
        final UUID notificationId = fromString(event.payloadAsJsonObject().getString(NOTIFICATION_ID));
        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString());
        final Optional<NotificationInfoResult> notificationInfoResult = notificationInfoJdbcRepository.findById(notificationId);

        if (notificationInfoResult.isPresent()) {
            final NotificationInfoResult notificationInfo = notificationInfoResult.get();
            final String notificationType = notificationInfo.getNotificationType();

            logger.info(">>CCT-2047 public.notificationnotify.events.notification-sent payload {} getNotificationType {} NotificationInfo {}",
                    event.asJsonObject(), notificationInfo.getNotificationType(), notificationInfo.getPayload());
            if (systemIdMapping.isPresent()) {
                JSONObject notificationPayload = new JSONObject(notificationInfo.getPayload());
                if (notificationPayload.has(RECIPIENT_TYPE)) {
                    generateAndAddDocument(event, systemIdMapping.get().getTargetId(), notificationPayload.getString(RECIPIENT_TYPE), notificationType);
                }
                notificationService.recordNotificationRequestSuccess(event, systemIdMapping.get().getTargetId(), CASE);
            } else {
                final Optional<SystemIdMapping> applicationSystemIdMapping = systemIdMapperService.getCppApplicationIdForNotificationId(notificationId.toString());
                if (applicationSystemIdMapping.isPresent()) {
                    notificationService.recordNotificationRequestSuccess(event, applicationSystemIdMapping.get().getTargetId(), APPLICATION);
                } else {
                    final Optional<SystemIdMapping> materialSystemIdMapping = systemIdMapperService.getCppMaterialIdForNotificationId(notificationId.toString());
                    if (materialSystemIdMapping.isPresent()) {
                        notificationService.recordNotificationRequestSuccess(event, materialSystemIdMapping.get().getTargetId(), MATERIAL);
                    } else {
                        logger.error("No Case, Application or Material found for the given notification id: {}", notificationId);
                    }
                }
            }
        } else {
            logger.error("NotificationInfo not found for notificationId: {}", notificationId);
        }
    }

    private void generateAndAddDocument(final JsonEnvelope event, final UUID caseId,
                                        final String recipientType, final String notificationType) {
        JsonObject emailDocumentJson = event.payloadAsJsonObject();
        emailDocumentJson = Json.createObjectBuilder(emailDocumentJson)
                .add(RECIPIENT_TYPE, recipientType)
                .add(NOTIFICATION_TYPE, notificationType).build();

        if (caseId == null || StringUtils.isEmpty(recipientType) || recipientType.equals("None")) {
            logger.error("Email or Letter Document is not generated as case id is {} and recipient Type is {}", caseId, recipientType);
            return;
        }

        try {
            final UUID materialId = randomUUID();
            final String fileName = format("%s notification of hearing %s %s copy", notificationType, formatter.format(LocalDateTime.now()), recipientType);

            documentGeneratorService.generateNonNowDocument(event, emailDocumentJson, EMAIL_DOCUMENT_TEMPLATE_NAME, materialId, fileName);
            hearingNotificationHelper.addCourtDocument(event, caseId, materialId, fileName);
        } catch (RuntimeException e) {

            logger.error("Error while generating and uploading email document case id : {} and exception is {}", caseId, e);
            logger.error("error :{}", e.getStackTrace());
            throw new RuntimeException(">> CCT-2047 error while generating email hearing template :{}");
        }
    }

    @Handles("progression.event.notification-request-failed")
    public void handleNotificationRequestFailed(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final String notificationId = payload.getString(NOTIFICATION_ID);

        logger.info(format("Attempting to clean-up temporary file related to failed notification id: %s", notificationId));
        deleteFile(UUID.fromString(notificationId));
    }

    @Handles("progression.event.notification-request-succeeded")
    public void handleNotificationRequestSucceeded(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String notificationId = payload.getString(NOTIFICATION_ID);
        final String materialId = payload.getString(MATERIAL_ID, null);
        final String completedAt = payload.getString(COMPLETED_AT, null);

        if (nonNull(completedAt) && nonNull(materialId)) {
            final Optional<SystemIdMapping> optionalSystemIdMapping = systemIdMapperService.getDocumentIdForMaterialId(materialId);

            optionalSystemIdMapping.ifPresent(mapping -> {
                final UUID courtDocumentId = mapping.getTargetId();
                final UpdateCourtDocumentPrintTime courtDocumentPrintTime = updateCourtDocumentPrintTime()
                        .withCourtDocumentId(courtDocumentId)
                        .withMaterialId(fromString(materialId))
                        .withPrintedAt(parse(completedAt))
                        .build();
                final Metadata metadata = metadataFrom(envelope.metadata())
                        .withName("progression.command.update-court-document-print-time")
                        .build();
                sender.send(envelopeFrom(metadata, courtDocumentPrintTime));
            });
        }

        logger.info(format("Attempting to clean-up temporary file related to successful notification id: %s", notificationId));
        deleteFile(UUID.fromString(notificationId));
    }

    private void deleteFile(final UUID notificationId) {
        try {
            fileStorer.delete(notificationId);
        } catch (final FileServiceException e) {
            logger.debug(format("Failed to delete file for given notification id: '%s' from FileService. This could be due to the notification not having an associated file.", notificationId), e);
        }
    }
}
