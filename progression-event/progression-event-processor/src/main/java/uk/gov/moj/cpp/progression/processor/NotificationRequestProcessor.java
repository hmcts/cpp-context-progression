package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Created by satishkumar on 12/11/2018.
 */
@SuppressWarnings({"WeakerAccess", "squid:S1160"})
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationRequestProcessor {
    private static final String MATERIAL_ID = "materialId";

    @Inject
    private NotificationService notificationService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Handles("progression.event.print-requested")
    public void printDocument(final JsonEnvelope event) {

        final JsonObject eventPayload = event.payloadAsJsonObject();

        final UUID notificationId = fromString(eventPayload.getString("notificationId"));

        final UUID materialId = fromString(eventPayload.getString(MATERIAL_ID));

        final boolean postage = eventPayload.containsKey("postage") && eventPayload.getBoolean("postage");

        notificationNotifyService.sendLetterNotification(event, notificationId, materialId, postage);

        notificationService.recordPrintRequestAccepted(event);
    }

    @Handles("progression.event.email-requested")
    public void emailDocument(final JsonEnvelope event) {

        final JsonObject eventPayload = event.payloadAsJsonObject();

        final JsonArray notifications = eventPayload.getJsonArray("notifications");

        notifications.forEach(notification -> notificationNotifyService.sendEmailNotification(event, (JsonObject)  notification));

        notificationService.recordEmailRequestAccepted(event);
    }

}
