package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by satishkumar on 12/11/2018.
 */
public class NotificationNotifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationNotifyService.class);

    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";
    private static final String NOTIFICATION_NOTIFY_LETTER_COMMAND = "notificationnotify.send-letter-notification";
    private static final String FIELD_LETTER_URL = "letterUrl";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_POSTAGE = "postage";
    private static final String POSTAGE_TYPE = "first";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    public void sendLetterNotification(final JsonEnvelope event, final UUID notificationId, final UUID materialId, final boolean postage) {

        final String letterUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final JsonObjectBuilder notificationBuilder = createObjectBuilder()
                .add(FIELD_LETTER_URL, letterUrl)
                .add(FIELD_NOTIFICATION_ID, notificationId.toString());

        if (postage) {
            notificationBuilder.add(FIELD_POSTAGE, POSTAGE_TYPE);
        }

        sender.sendAsAdmin(
                envelopeFrom(
                        metadataFrom(event.metadata()).withName(NOTIFICATION_NOTIFY_LETTER_COMMAND),
                        notificationBuilder.build()
                )
        );
    }

    public void sendEmailNotification(final JsonEnvelope event, final JsonObject emailNotification) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending - {} ", emailNotification);
        }

        sender.sendAsAdmin(
                envelopeFrom(
                        metadataFrom(event.metadata()).withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE),
                        emailNotification
                )
        );
    }
}
