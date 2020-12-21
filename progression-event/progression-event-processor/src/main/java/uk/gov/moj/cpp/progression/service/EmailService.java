package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;

import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class EmailService {
    private static final String URI_TO_MATERIAL = "defence/case/materials?caseId=%s&material=%s";
    private static final String MATERIAL_SECTIONS_URL = "MATERIAL_SECTIONS_URL";
    private static final String URN = "URN";
    private static final String DEFENDANT_PATH_PARAM = "&defendantId=";

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private NotificationService notificationService;

    @Inject
    private ApplicationParameters applicationParameters;

    public void sendEmailNotifications(final JsonEnvelope jsonEnvelope,
                                       final UUID notificationId,
                                       final UUID materialId,
                                       final String urn,
                                       final UUID caseId,
                                       final Map<UUID, String> defendantAndRelatedOrganisationEmail) {
        if (!defendantAndRelatedOrganisationEmail.isEmpty()) {
            final String urlLink = format(URI_TO_MATERIAL, caseId, materialId);
            final String linkToMaterialPage = applicationParameters.getEndClientHost().concat(urlLink);
            final List<EmailChannel> emailChannelList = defendantAndRelatedOrganisationEmail.keySet()
                    .stream().map(x -> buildEmailChannel(defendantAndRelatedOrganisationEmail.get(x),
                            urn, linkToMaterialPage, x.toString())).collect(Collectors.toList());
            notificationService.sendEmail(jsonEnvelope, notificationId, caseId, null, materialId, emailChannelList);
        }
    }

    private EmailChannel buildEmailChannel(final String emailAddress,
                                           final String urn,
                                           final String url,
                                           final String defendantId) {

        final Personalisation personalisation = Personalisation.personalisation()
                .withAdditionalProperty(URN, urn)
                .withAdditionalProperty(MATERIAL_SECTIONS_URL, url.concat(DEFENDANT_PATH_PARAM).concat(defendantId))
                .build();

        return EmailChannel.emailChannel()
                .withPersonalisation(personalisation)
                .withSendToAddress(emailAddress)
                .withTemplateId(fromString(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()))
                .build();
    }
}
