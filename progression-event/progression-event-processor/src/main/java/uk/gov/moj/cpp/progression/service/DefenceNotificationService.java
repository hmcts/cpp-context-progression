package uk.gov.moj.cpp.progression.service;


import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DefenceNotificationService {

    private static final String URI_TO_MATERIAL = "prosecution-casefile/case-materials?caseId=%s&material=%s";
    private static final String MATERIAL_SECTIONS_URL = "MATERIAL_SECTIONS_URL";
    private static final String URN = "URN";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private DefenceService defenceService;
    @Inject
    private UsersGroupService usersGroupService;
    @Inject
    private NotificationService notificationService;
    @Inject
    private ApplicationParameters applicationParameters;

    public void prepareNotificationsForCourtDocument(final JsonEnvelope jsonEnvelope, final CourtDocument courtDocument) {

       processCaseDocument(jsonEnvelope, courtDocument.getDocumentCategory().getCaseDocument(), courtDocument.getMaterials().get(0).getId());
       processDefendantDocument(jsonEnvelope, courtDocument.getDocumentCategory().getDefendantDocument(), courtDocument.getMaterials().get(0).getId());


    }

    private void processDefendantDocument(final JsonEnvelope jsonEnvelope, final DefendantDocument defendantDocument, final UUID materialId) {

        if(defendantDocument != null) {
            final CaseDefendantsWithOrganisation caseDefendantsWithOrganisation = defenceService.getDefendantsAndAssociatedOrganisationsForCase(jsonEnvelope, defendantDocument.getProsecutionCaseId().toString())
                    .getCaseDefendantOrganisation();
            final String urn = caseDefendantsWithOrganisation.getUrn();
            final UUID caseId = caseDefendantsWithOrganisation.getCaseId();
            final List<String> organisationIds =  caseDefendantsWithOrganisation.getDefendants().stream()
                    .filter(x -> defendantDocument.getDefendants().contains(x.getDefendantId()))
                    .filter(x -> x.getAssociatedOrganisation() != null)
                    .map(x -> x.getAssociatedOrganisation().toString())
                    .distinct()
                    .collect(Collectors.toList());
            if(!organisationIds.isEmpty()) {
                final List<String> emailIst = usersGroupService.getEmailsForOrganisationIds(jsonEnvelope, organisationIds);
                sendNotificationToEmailsForCase(jsonEnvelope, emailIst, caseId.toString(), urn, materialId);
            }
        }
    }

    private void processCaseDocument(final JsonEnvelope jsonEnvelope, final CaseDocument caseDocument, final UUID materialId) {
        if(caseDocument != null) {
            final CaseDefendantsWithOrganisation caseDefendantsWithOrganisation = defenceService.getDefendantsAndAssociatedOrganisationsForCase(jsonEnvelope, caseDocument.getProsecutionCaseId().toString())
              .getCaseDefendantOrganisation();
            final String urn = caseDefendantsWithOrganisation.getUrn();
            final UUID caseId = caseDefendantsWithOrganisation.getCaseId();
            final List<String> organisationIds =  caseDefendantsWithOrganisation.getDefendants().stream()
                    .filter(x -> x.getAssociatedOrganisation() != null)
                    .map(x -> x.getAssociatedOrganisation().toString())
                    .distinct()
                    .collect(Collectors.toList());
            if(!organisationIds.isEmpty()) {
                final List<String> emailIst = usersGroupService.getEmailsForOrganisationIds(jsonEnvelope, organisationIds);
                sendNotificationToEmailsForCase(jsonEnvelope, emailIst, caseId.toString(), urn, materialId);
            }

        }
    }

    private void sendNotificationToEmailsForCase(final JsonEnvelope jsonEnvelope, final List<String> emailsList, final String caseId, final String urn, final UUID materialId){
        final String linkToMaterialPage = applicationParameters.getEndClientHost().concat(format(URI_TO_MATERIAL, caseId, materialId));
        final List<EmailChannel> emailChannelList = emailsList.stream().map(x  -> buildEmailChannel(x, urn, linkToMaterialPage)).collect(Collectors.toList());
        notificationService.sendEmail(jsonEnvelope, randomUUID(), fromString(caseId),  null,  null, emailChannelList,  null);
    }

    private EmailChannel buildEmailChannel(final String emailAddress, final String urn, final String url){
        final Personalisation personalisation = Personalisation.personalisation()
                                                  .withAdditionalProperty(URN, urn)
                                                  .withAdditionalProperty(MATERIAL_SECTIONS_URL, url)
                                                  .build();
        return EmailChannel.emailChannel()
                .withPersonalisation(personalisation)
                .withSendToAddress(emailAddress)
                .withTemplateId(fromString(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()))
                .build();


    }
}
