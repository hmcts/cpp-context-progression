package uk.gov.moj.cpp.progression.nows;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * this is a proxy for notificationnotify.email in notify context
 * which is itself a proxy for external service sendEmail uk.gov.service.notify.NotificationRouter
 */
@SuppressWarnings({"squid:S2221","squid:S1162","squid:S2259"})
public class EmailNowNotificationChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNowNotificationChannel.class);

    public static final String NOTIFICATIONNOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";
    public static final String TEMPLATE_ID_PROPERTY_NAME = "templateId";
    public static final String FROM_ADDRESS_PROPERTY_NAME = "fromAddress";
    public static final String CASE_URNS_PERSONALISATION_KEY = "caseUrns";
    public static final String COURT_CLERK_NAME_PERSONALISATION_KEY = "courtClerkName";
    private static final String SUBJECT_PERSONALISATION_KEY = "subject";
    private static final String STATUS_MESSAGE_PERSONALISATION_KEY = "statusMessage";
    private static final String STATUS_PERSONALISATION_KEY = "status";
    private static final String NOWS_TYPE_NAME_PERSONALISATION_KEY = "nowsTypeName";
    private static final String NOWS_GENERATED_DATE_PERSONALISATION_KEY = "nowsGeneratedDate";
    private static final String NOWS_GENERATED_TIME_PERSONALISATION_KEY = "nowsGeneratedTime";
    private static final String COURT_CENTRE_NAME_PERSONALISATION_KEY = "courtCentreName";
    private static final String NOWS_URL_PERSONALISATION_KEY = "nowsUrl";
    private static final String NOWS_PDF_URL = "(%s/material-query-api/query/api/rest/material/material/%s?requestPdf=true)";
    private static final String URGENT = "STATUS: URGENT";
    private static final String EMPTY = "";
    private static final String URGENT_MESSAGE = "The listed notice, order or warrant has been marked as URGENT and requires your immediate attention.";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public static final String EMAIL_TYPE = "email";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Inject
    @Value(key = "materialExternalWebLinkBaseUrl")
    private String materialExternalWebLinkBaseUrl;

    public void notify(final Sender sender, final JsonEnvelope event, String destination, Map<String, String> properties, NotificationDocumentState nowsNotificationDocumentState) throws InvalidNotificationException {

        final Notification emailNotification = new Notification();
        emailNotification.setNotificationId(UUID.randomUUID());

        emailNotification.setSendToAddress(destination);
        final String templateId = properties.get(TEMPLATE_ID_PROPERTY_NAME);

        if(isNull(templateId)) {
            throw new InvalidNotificationException(String.format("Null template id for \"%s\"", emailNotification.getSendToAddress()));
        }

        final boolean urgent = isNowTypeUrgent(nowsNotificationDocumentState.getPriority());

        final String caseUrns = nowsNotificationDocumentState.getCaseUrns().stream().collect(Collectors.joining(","));

        final String nowTypeName = nowsNotificationDocumentState.getOrderName();

        final LocalDateTime now = LocalDateTime.now();

        final Map<String, String> personalisation = new HashMap<>();
        personalisation.put(CASE_URNS_PERSONALISATION_KEY, caseUrns);
        personalisation.put(COURT_CLERK_NAME_PERSONALISATION_KEY, nowsNotificationDocumentState.getCourtClerkName());
        personalisation.put(SUBJECT_PERSONALISATION_KEY, createSubject(nowTypeName, caseUrns, urgent));
        personalisation.put(STATUS_MESSAGE_PERSONALISATION_KEY, urgent ? URGENT_MESSAGE : EMPTY);
        personalisation.put(STATUS_PERSONALISATION_KEY, urgent ? URGENT : EMPTY);
        personalisation.put(NOWS_TYPE_NAME_PERSONALISATION_KEY, nowsNotificationDocumentState.getOrderName());
        personalisation.put(NOWS_GENERATED_DATE_PERSONALISATION_KEY, now.format(dateFormatter));
        personalisation.put(NOWS_GENERATED_TIME_PERSONALISATION_KEY, now.format(timeFormatter));
        personalisation.put(COURT_CENTRE_NAME_PERSONALISATION_KEY, nowsNotificationDocumentState.getCourtCentreName());
        personalisation.put(NOWS_URL_PERSONALISATION_KEY, getNowsPdfFileUrl(nowsNotificationDocumentState.getMaterialId()));
        emailNotification.setPersonalisation(personalisation);

        try {
            emailNotification.setTemplateId(UUID.fromString(templateId));
        } catch (IllegalArgumentException ex) {
            throw new InvalidNotificationException(String.format("cant notify %s invalid template id: \"%s\"", emailNotification.getSendToAddress(), templateId), ex);
        }

        final String replyToAddress = properties.get(FROM_ADDRESS_PROPERTY_NAME);

        emailNotification.setReplyToAddress(replyToAddress);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending - {} ", toString(emailNotification));
        }

        try {
            sender.sendAsAdmin(this.enveloper.withMetadataFrom(event, NOTIFICATIONNOTIFY_EMAIL_METADATA_TYPE)
                    .apply(this.objectToJsonObjectConverter.convert(emailNotification)));

        } catch (RuntimeException ex) {
            LOGGER.error("Exception occurred by sending email - {} ", toString(emailNotification), ex);
        }
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

    private boolean isNowTypeUrgent(String priority) {
        final int URGENT_PRIORITY = 30;
        try {
            return nonNull(priority) && Integer.parseInt(priority) <= URGENT_PRIORITY;
        } catch (NumberFormatException ex) {
            LOGGER.error("Exception occurred while converting priority - {} ", priority, ex);
        }
        return false;
    }

    private String createSubject(final String nowTypeName, final String caseUrns, final boolean urgent) {
        return urgent ?
                String.format("%s - %s - %s", URGENT, nowTypeName, caseUrns) :
                String.format("%s - %s", nowTypeName, caseUrns);
    }

    private String getNowsPdfFileUrl(final UUID materialId) {
        return String.format(NOWS_PDF_URL, materialExternalWebLinkBaseUrl, materialId.toString());
    }
}
