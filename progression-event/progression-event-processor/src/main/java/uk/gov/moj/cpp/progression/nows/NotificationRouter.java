package uk.gov.moj.cpp.progression.nows;

import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import java.util.Map;


public class NotificationRouter {

    private final EmailNowNotificationChannel emailNowNotificationChannel;

    @Inject
    NotificationRouter(final EmailNowNotificationChannel emailNowNotificationChannel) {
        this.emailNowNotificationChannel = emailNowNotificationChannel;
    }

    public void notify(final Sender sender, final JsonEnvelope event, String destination,
                       String channelType, Map<String, String> properties, NotificationDocumentState nowsNotificationDocumentState) throws InvalidNotificationException {

        if (EmailNowNotificationChannel.EMAIL_TYPE.equals(channelType)) {
            emailNowNotificationChannel.notify(sender, event, destination, properties, nowsNotificationDocumentState);
        } else {
            throw new IllegalArgumentException("invalid channel type: " + channelType);
        }
    }


}
