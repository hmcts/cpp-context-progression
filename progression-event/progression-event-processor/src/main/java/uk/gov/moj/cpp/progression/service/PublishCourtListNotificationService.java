package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.Personalisation.personalisation;
import static uk.gov.justice.core.courts.notification.EmailChannel.emailChannel;
import static uk.gov.justice.listing.courts.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.courts.PublishCourtListType.WARN;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableList;

public class PublishCourtListNotificationService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS");
    private static final String HEARING_NOTIFICATION_DATE = "hearing_notification_date";

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public void sendNotification(final JsonEnvelope event, final PublishCourtListPayload publishCourtListPayload, final String documentTemplateName) {
        final JsonObject documentPayload = objectToJsonObjectConverter.convert(publishCourtListPayload);
        final UUID materialId = randomUUID();
        documentGeneratorService.generateNonNowDocument(event, documentPayload, documentTemplateName, materialId, getNotificationPdfName(documentTemplateName));
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        final UUID notificationId = randomUUID();
        if (isNotBlank(publishCourtListPayload.getAddressee().getEmail())) {
            final EmailChannel emailChannel = emailChannel().withPersonalisation(personalisation()
                            .withAdditionalProperty(HEARING_NOTIFICATION_DATE, publishCourtListPayload.getIssueDate())
                            .build())
                    .withMaterialUrl(materialUrl)
                    .withTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()))
                    .withSendToAddress(publishCourtListPayload.getAddressee().getEmail())
                    .build();
            notificationService.sendEmail(event, notificationId, null, null, materialId, ImmutableList.of(emailChannel));
        } else if (publishCourtListPayload.getPublishCourtListType() == WARN || publishCourtListPayload.getPublishCourtListType() == FIRM) {
            notificationService.sendLetter(event, notificationId, null, null, materialId, true);
        }
    }

    private String getNotificationPdfName(final String templateName) {
        return templateName + "_" + formatter.format(LocalDateTime.now());
    }
}
