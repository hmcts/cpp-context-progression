package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;

import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.payloads.Defendants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class EmailService {
    private static final String URI_TO_MATERIAL = "defence/case/materials/%s/%s/defending?advocate=true";
    private static final String MATERIAL_SECTIONS_URL = "MATERIAL_SECTIONS_URL";
    private static final String URN = "URN";
    private static final String DEFENDANT_PATH_PARAM = "&defendantId=";
    private static final String DEFENDANT_LIST = "defendant_list";
    private static final String DOCUMENT_TITLE = "DOCUMENT_TITLE";
    private static final String DOCUMENT_SECTION = "DOCUMENT_SECTION";

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private NotificationService notificationService;

    @Inject
    private ApplicationParameters applicationParameters;

    public void sendEmailNotifications(final JsonEnvelope jsonEnvelope,
                                       final UUID materialId,
                                       final String urn,
                                       final UUID caseId,
                                       final Map<Defendants, String> defendantAndRelatedOrganisationEmail,
                                       final String documentSection,
                                       final String documentName) {
        if (!defendantAndRelatedOrganisationEmail.isEmpty()) {
            final String urlLink = format(URI_TO_MATERIAL, urn, caseId);
            final String linkToMaterialPage = applicationParameters.getEndClientHost().concat(urlLink);
            final List<EmailChannel> emailChannelList = defendantAndRelatedOrganisationEmail.keySet()
                    .stream().map(x -> buildEmailChannel(defendantAndRelatedOrganisationEmail.get(x),
                            urn, linkToMaterialPage, x.getDefendantId().toString(), getDefendantNameList(defendantAndRelatedOrganisationEmail.keySet()), documentSection, documentName)).collect(Collectors.toList());
            notificationService.sendEmail(jsonEnvelope, caseId, null, materialId, emailChannelList);
        }
    }

    private String getDefendantNameList(Set<Defendants> defendantsSet) {
        return defendantsSet.stream().map(defendant -> defendant.getDefendantFullName().trim().isEmpty() ? defendant.getOrganisationName(): defendant.getDefendantFullName()).collect(Collectors.joining(", "));
    }

    private EmailChannel buildEmailChannel(final String emailAddress,
                                           final String urn,
                                           final String url,
                                           final String defendantId,
                                           final String defendantList,
                                           final String documentSection,
                                           final String documentName
    ) {

        final Personalisation personalisation = Personalisation.personalisation()
                .withAdditionalProperty(URN, urn)
                .withAdditionalProperty(MATERIAL_SECTIONS_URL, url.concat(DEFENDANT_PATH_PARAM).concat(defendantId))
                .withAdditionalProperty(DEFENDANT_LIST, defendantList)
                .withAdditionalProperty(DOCUMENT_TITLE, documentName)
                .withAdditionalProperty(DOCUMENT_SECTION, documentSection)
                .build();

        return EmailChannel.emailChannel()
                .withPersonalisation(personalisation)
                .withSendToAddress(emailAddress)
                .withTemplateId(fromString(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()))
                .build();
    }
}
