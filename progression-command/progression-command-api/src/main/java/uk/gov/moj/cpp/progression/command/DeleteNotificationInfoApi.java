package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.DeleteNotificationInfoService;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class DeleteNotificationInfoApi {

    public static final String DELETE_NOTIFICATION_INFO = "progression.delete-notification-info";
    public static final String NOTIFICATION_STATUS = "PROCESSED";

    @Inject
    private DeleteNotificationInfoService deleteNotificationInfoService;

    @Inject
    private Enveloper enveloper;

    @Handles(DELETE_NOTIFICATION_INFO)
    public Envelope handle(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        deleteNotificationInfoService.deleteNotifications(NOTIFICATION_STATUS, ZonedDateTime.now());

        return envelop(payload)
                .withName(DELETE_NOTIFICATION_INFO)
                .withMetadataFrom(envelope);
    }

}
