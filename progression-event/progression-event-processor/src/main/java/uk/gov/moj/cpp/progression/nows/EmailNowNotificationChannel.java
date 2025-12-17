package uk.gov.moj.cpp.progression.nows;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is a proxy for notification-notify.email in notify context which is itself a proxy for
 * external service sendEmail uk.gov.service.notify.NotificationRouter
 */

public class EmailNowNotificationChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNowNotificationChannel.class);

    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    public void notify(final Sender sender, final JsonEnvelope event, final Notification emailNotification) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending - {} ", toString(emailNotification));
        }

        sender.sendAsAdmin(this.enveloper.withMetadataFrom(event, NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE)
                .apply(this.objectToJsonObjectConverter.convert(emailNotification)));

    }

    private String toString(Notification notification) {
        return String.format("to: %s from: %s templateId: %s notificationId: %s personalization: %s",
                notification.getSendToAddress(),
                notification.getReplyToAddress(),
                notification.getTemplateId(),
                notification.getNotificationId(),
                notification.getPersonalisation().entrySet().stream()
                        .map(entry -> "" + entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(","))
        );
    }
}
