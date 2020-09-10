package uk.gov.moj.cpp.progression.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S00112")
public class NCESNotificationRequestedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NCESNotificationRequestedEventProcessor.class);
    private final Sender sender;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private DocumentGeneratorService documentGeneratorService;
    private NotificationService notificationService;
    private MaterialUrlGenerator materialUrlGenerator;
    private ApplicationParameters applicationParameters;

    @Inject
    public NCESNotificationRequestedEventProcessor(final Sender sender,
                                                   final DocumentGeneratorService documentGeneratorService,
                                                   final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                                   final MaterialUrlGenerator materialUrlGenerator,
                                                   final NotificationService notificationService,
                                                   final ApplicationParameters applicationParameters
    ) {
        this.sender = sender;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.documentGeneratorService = documentGeneratorService;
        this.notificationService = notificationService;
        this.materialUrlGenerator = materialUrlGenerator;
        this.applicationParameters=applicationParameters;
    }


    @Handles("public.hearing.event.nces-notification-requested")
    public void processPublicNCESNotificationRequested(final JsonEnvelope event) {
        final UUID userId = fromString(event.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final JsonObject requestJson = event.payloadAsJsonObject();
        final NcesNotificationRequested ncesNotificationRequested = jsonObjectToObjectConverter.convert(requestJson, NcesNotificationRequested.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Nces notification requested payload - {}", requestJson);
        }
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(ncesNotificationRequested.getMaterialId());
        final List<EmailChannel> emailChannelList = ncesNotificationRequested.getEmailNotifications().stream()
                                                      .map(s -> EmailChannel.emailChannel()
                                                                        .withTemplateId(UUID.fromString(applicationParameters.getNcesEmailTemplateId()))
                                                                        .withSendToAddress(s.getSendToAddress())
                                                                        .withReplyToAddress(s.getReplyToAddress())
                                                              .withMaterialUrl(materialUrl)
                                                                        .withPersonalisation(Personalisation.personalisation()
                                                                                .withAdditionalProperty("subject",
                                                                                        ncesNotificationRequested.getDocumentContent().getAmendmentType())
                                                                                                     .build())
                                                                        .build())
                                                      .collect(Collectors.toList());

        documentGeneratorService.generateNcesDocument(sender, event, userId, ncesNotificationRequested);
        notificationService.sendEmail(event, UUID.randomUUID(), ncesNotificationRequested.getCaseId(), null,
                ncesNotificationRequested.getMaterialId(),
                emailChannelList);

    }

}

