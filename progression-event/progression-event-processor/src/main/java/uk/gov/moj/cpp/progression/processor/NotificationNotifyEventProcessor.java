package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime.updateCourtDocumentPrintTime;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.APPLICATION;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.CASE;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.MATERIAL;

import uk.gov.justice.core.courts.UpdateCourtDocumentPrintTime;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

// squid:S1312 - logger not being static final
// squid:S2629 - logger not being called conditionally
@SuppressWarnings({"WeakerAccess", "squid:S2629", "squid:S1312"})
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationNotifyEventProcessor {

    private static final String NOTIFICATION_ID = "notificationId";
    private static final String MATERIAL_ID = "materialId";
    private static final String COMPLETED_AT = "completedAt";

    @Inject
    private NotificationService notificationService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

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

        if (systemIdMapping.isPresent()) {
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
                    logger.info(format("No Case, Application or Material found for the given notification id: %s", notificationId));
                }
            }
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
