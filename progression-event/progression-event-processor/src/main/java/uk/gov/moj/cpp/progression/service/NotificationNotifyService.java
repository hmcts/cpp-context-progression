package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
/**
 * Created by satishkumar on 12/11/2018.
 */
public class NotificationNotifyService {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    public void sendLetterNotification(final JsonEnvelope event, final UUID notificationId, final UUID materialId) {

        final String letterUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final JsonObject payload = createObjectBuilder()
                .add("letterUrl", letterUrl)
                .add("notificationId", notificationId.toString())
                .build();

        sender.sendAsAdmin(enveloper.withMetadataFrom(event, "notificationnotify.send-letter-notification")
                .apply(payload)
        );
    }
}
