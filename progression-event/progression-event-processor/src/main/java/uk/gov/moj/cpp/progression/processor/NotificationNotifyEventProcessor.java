package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.APPLICATION;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.CASE;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.MATERIAL;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationNotifyEventProcessor {

    private static final String NOTIFICATION_ID = "notificationId";

    @Inject
    private NotificationService notificationService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @SuppressWarnings({"squid:S1312"}) //supressing Sonar warning of logger not being static final
    @Inject
    private Logger logger;

    @SuppressWarnings({"squid:S2629"})
    //supressing Sonar warning of logger not being called conditionally
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

    @SuppressWarnings({"squid:S2629"})
    //supressing Sonar warning of logger not being called conditionally
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
}
