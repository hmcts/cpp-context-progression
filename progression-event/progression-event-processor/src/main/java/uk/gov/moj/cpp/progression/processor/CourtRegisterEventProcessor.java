package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.helper.CourtRegisterHelper.getCourtRegisterStreamId;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class CourtRegisterEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtRegisterEventProcessor.class.getName());

    public static final String COURT_REGISTER_TEMPLATE = "OEE_Layout5";
    private static final String FIELD_COURT_REGISTER_DOCUMENT_REQUESTS = "courtRegisterDocumentRequests";
    private static final String FILE_NAME = "fileName";
    private static final String FIELD_RECIPIENTS = "recipients";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String FILE_ID = "fileId";
    private static final String PERSONALISATION = "personalisation";
    private static final String RECIPIENT = "yotsName";
    private static final String RECIPIENT_NAME = "recipientName";
    private static final String EMAIL_ADDRESS = "emailAddress1";
    private static final String EMAIL_TEMPLATE_NAME = "emailTemplateName";
    public static final String PDF = "pdf";

    @Inject
    private FileStorer fileStorer;

    @Inject
    private CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private Sender sender;

    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_REGISTER_DATE = "registerDate";


    @SuppressWarnings({"squid:S1160", "squid:S3655"})
    @Handles("progression.event.court-register-generated")
    public void generateCourtRegister(final JsonEnvelope envelope) throws FileServiceException {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final List<JsonObject> courtRegisterDocumentRequests = payload.getJsonArray(FIELD_COURT_REGISTER_DOCUMENT_REQUESTS).getValuesAs(JsonObject.class);
        final String fileName = courtRegisterDocumentRequests.get(0).getString(FILE_NAME);
        final JsonObject courtRegisterGeneratorPayload = courtRegisterPdfPayloadGenerator.mapPayload(payload);
        final UUID fileId = storeCourtRegisterGeneratorPayload(courtRegisterGeneratorPayload, fileName);

        courtRegisterDocumentRequests.stream().findAny().ifPresent(crdRequest -> {
            final String courtCentreId = crdRequest.getString(FIELD_COURT_CENTRE_ID);
            final String registerDate = ZonedDateTime.parse(crdRequest.getString(FIELD_REGISTER_DATE)).toLocalDate().toString();
            this.requestDocumentGeneration(envelope, getCourtRegisterStreamId(courtCentreId, registerDate), fileId);
        });

    }

    @Handles("progression.event.court-register-notified")
    public void notifyCourt(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(FIELD_RECIPIENTS)) {
            final List<JsonObject> recipients = payload.getJsonArray(FIELD_RECIPIENTS).getValuesAs(JsonObject.class);
            if(nonNull(recipients)) {
                recipients.forEach(rp -> {
                    final String templateId = applicationParameters.getEmailTemplateId(rp.getString(EMAIL_TEMPLATE_NAME));
                    if (isNotBlank(templateId)) {
                        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
                        final JsonString fileId = payload.getJsonString("systemDocGeneratorId");
                        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, randomUUID().toString());
                        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, templateId);
                        notifyObjectBuilder.add(SEND_TO_ADDRESS, rp.getJsonString(EMAIL_ADDRESS));
                        notifyObjectBuilder.add(FILE_ID, fileId);
                        notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder().add(RECIPIENT, rp.getString(RECIPIENT_NAME)).build());
                        this.notificationNotifyService.sendEmailNotification(envelope, notifyObjectBuilder.build());
                    } else {
                        LOGGER.info("Court register notification is not sent due to missing template Id");
                    }
                });
            }
        }
    }

    @Handles("progression.event.court-register-notified-v2")
    public void notifyCourtV2(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(FIELD_RECIPIENTS)) {
            final List<JsonObject> recipients = payload.getJsonArray(FIELD_RECIPIENTS).getValuesAs(JsonObject.class);
            if(nonNull(recipients)) {
                recipients.forEach(rp -> {
                    final String templateId = applicationParameters.getEmailTemplateId(rp.getString(EMAIL_TEMPLATE_NAME));
                    if (isNotBlank(templateId)) {
                        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
                        final JsonString fileId = payload.getJsonString("systemDocGeneratorId");
                        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, randomUUID().toString());
                        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, templateId);
                        notifyObjectBuilder.add(SEND_TO_ADDRESS, rp.getJsonString(EMAIL_ADDRESS));
                        notifyObjectBuilder.add(FILE_ID, fileId);
                        notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder().add(RECIPIENT, rp.getString(RECIPIENT_NAME)).build());
                        this.notificationNotifyService.sendEmailNotification(envelope, notifyObjectBuilder.build());
                    } else {
                        LOGGER.info("Court register notification is not sent due to missing template Id");
                    }
                });
            }
        }
    }

    public static byte[] jsonObjectAsByteArray(final JsonObject jsonObject) {
        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }


    private void requestDocumentGeneration(final JsonEnvelope eventEnvelope,
                                           final UUID courtCentreStreamId,
                                           final UUID payloadFileServiceUUID) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add("originatingSource", "CourtRegister")
                .add("templateIdentifier", COURT_REGISTER_TEMPLATE)
                .add("conversionFormat", PDF)
                .add("sourceCorrelationId", courtCentreStreamId.toString())
                .add("payloadFileServiceId", payloadFileServiceUUID.toString())
                .build();

        sender.sendAsAdmin(
                Envelope.envelopeFrom(
                        metadataFrom(eventEnvelope.metadata()).withName("systemdocgenerator.generate-document"),
                        docGeneratorPayload
                )
        );
    }

    private UUID storeCourtRegisterGeneratorPayload(final JsonObject courtRegisterGeneratorPayload, final String fileName) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(courtRegisterGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add(FILE_NAME, fileName)
                .add("conversionFormat", PDF)
                .add("templateName", COURT_REGISTER_TEMPLATE)
                .add("numberOfPages", 1)
                .add("fileSize", jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

}
