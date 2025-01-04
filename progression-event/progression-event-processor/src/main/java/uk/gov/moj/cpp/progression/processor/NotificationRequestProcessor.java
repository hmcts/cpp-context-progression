package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.empty;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.Country;
import uk.gov.moj.cpp.progression.events.NotificationSentForPleaDocument;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"WeakerAccess", "squid:S1160"})
@ServiceComponent(EVENT_PROCESSOR)
public class NotificationRequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRequestProcessor.class);
    private static final String MATERIAL_ID = "materialId";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String FILE_ID = "fileId";
    private static final String URN = "urn";
    private static final String PERSONALISATION = "personalisation";

    @Inject
    private NotificationService notificationService;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private ApplicationParameters applicationParameters;

    @Handles("progression.event.print-requested")
    public void printDocument(final JsonEnvelope event) {

        final JsonObject eventPayload = event.payloadAsJsonObject();

        final UUID notificationId = fromString(eventPayload.getString(FIELD_NOTIFICATION_ID));

        final UUID materialId = fromString(eventPayload.getString(MATERIAL_ID));

        final boolean postage = eventPayload.containsKey("postage") && eventPayload.getBoolean("postage");

        notificationNotifyService.sendLetterNotification(event, notificationId, materialId, postage);

        notificationService.recordPrintRequestAccepted(event);
    }

    @Handles("progression.event.email-requested")
    public void emailDocument(final JsonEnvelope event) {

        final JsonObject eventPayload = event.payloadAsJsonObject();

        final JsonArray notifications = eventPayload.getJsonArray("notifications");

        notifications.forEach(notification -> notificationNotifyService.sendEmailNotification(event, (JsonObject) notification));

        notificationService.recordEmailRequestAccepted(event);
    }

    @Handles("progression.event.notification-sent-for-plea-document")
    public void handleNotificationSentForPleaDocument(final JsonEnvelope event) {
        final NotificationSentForPleaDocument notificationSentForPleaDocument = jsonObjectConverter.convert(event.payloadAsJsonObject(), NotificationSentForPleaDocument.class);
        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, notificationSentForPleaDocument.getNotificationId().toString());
        notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder()
                .add(URN, notificationSentForPleaDocument.getUrn())
                .build());
        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, applicationParameters.getOnlinePleaProsecutorTemplateId());
        notifyObjectBuilder.add(SEND_TO_ADDRESS, notificationSentForPleaDocument.getEmail());
        notifyObjectBuilder.add(FILE_ID, notificationSentForPleaDocument.getSystemDocGeneratorId().toString());
        this.notificationNotifyService.sendEmailNotification(event, notifyObjectBuilder.build());
    }

    @Handles("progression.event.notification-sent-for-defendant-document")
    public void notifyDefendantAboutPleaSubmission(final JsonEnvelope event) {
        final JsonObject eventPayload = event.payloadAsJsonObject();
        final String email = eventPayload.getString("email");
        final String urn = eventPayload.getString("urn");
        final String postcode = eventPayload.getString("postcode");
        final UUID notificationId = fromString(eventPayload.getString(FIELD_NOTIFICATION_ID));
        final String templateType = eventPayload.getString("templateType");
        final String templateId = getTemplateId(templateType, event, postcode);

        notificationNotifyService.sendEmailNotification(event.metadata(), urn, email, notificationId, templateId, empty());
    }

    private String getTemplateId(final String templateType, final JsonEnvelope event, final String postcode) {

        switch (templateType) {
            case "onlineGuiltyPleaCourtHearing":
                return isWelsh(event, postcode) ? applicationParameters.getOnlineGuiltyPleaCourtHearingWelshTemplateId() : applicationParameters.getOnlineGuiltyPleaCourtHearingEnglishTemplateId();

            case "onlineGuiltyPleaNoCourtHearing":
                return isWelsh(event, postcode) ? applicationParameters.getOnlineGuiltyPleaNoCourtHearingWelshTemplateId() : applicationParameters.getOnlineGuiltyPleaNoCourtHearingEnglishTemplateId();

            case "onlineNotGuiltyPlea":
                return isWelsh(event, postcode) ? applicationParameters.getOnlineNotGuiltyPleaWelshTemplateId() : applicationParameters.getOnlineNotGuiltyPleaEnglishTemplateId();

            default:
                return null;
        }
    }

    private boolean isWelsh(final JsonEnvelope event, final String postcode) {
        final String country = referenceDataService.getCountryByPostcode(event, postcode, requester);
        LOGGER.info("Retrieved Country By Postcode {} is {}", postcode, country);
        return Country.WALES.getName().equalsIgnoreCase(country);
    }
}


