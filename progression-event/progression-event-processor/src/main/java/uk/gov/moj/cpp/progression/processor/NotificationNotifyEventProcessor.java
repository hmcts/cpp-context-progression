package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.PrintService;
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
    private PrintService printService;

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
        if (!systemIdMapping.isPresent()) {
            logger.error(format("No case found for the given notification id: %s", notificationId));
        } else {
            printService.recordPrintRequestFailure(event, systemIdMapping.get().getTargetId());
        }
    }

    @SuppressWarnings({"squid:S2629"})
    //supressing Sonar warning of logger not being called conditionally
    @Handles("public.notificationnotify.events.notification-sent")
    public void markNotificationAsSucceeded(final JsonEnvelope event) {
        final UUID notificationId = fromString(event.payloadAsJsonObject().getString(NOTIFICATION_ID));
        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperService.getCppCaseIdForNotificationId(notificationId.toString());
        if (!systemIdMapping.isPresent()) {
            logger.error(format("No case found for the given notification id: %s", notificationId));
        } else {
            printService.recordPrintRequestSuccess(event, systemIdMapping.get().getTargetId());
        }
    }
}
