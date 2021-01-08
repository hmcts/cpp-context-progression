package uk.gov.moj.cpp.progression.service;


import static java.util.UUID.randomUUID;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.Defendants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DefenceNotificationService {

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private DefenceService defenceService;

    @Inject
    private UsersGroupService usersGroupService;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private EmailService emailService;

    public void prepareNotificationsForCourtDocument(final JsonEnvelope jsonEnvelope, final CourtDocument courtDocument) {

        processCaseDocument(jsonEnvelope, courtDocument.getDocumentCategory().getCaseDocument(), courtDocument.getMaterials().get(0).getId());
        processDefendantDocument(jsonEnvelope, courtDocument.getDocumentCategory().getDefendantDocument(), courtDocument.getMaterials().get(0).getId());
    }

    private void processDefendantDocument(final JsonEnvelope jsonEnvelope, final DefendantDocument defendantDocument, final UUID materialId) {

        if (defendantDocument != null) {
            final CaseDefendantsWithOrganisation caseDefendantsWithOrganisation =
                    defenceService.getDefendantsAndAssociatedOrganisationsForCase(jsonEnvelope, defendantDocument.getProsecutionCaseId().toString())
                            .getCaseDefendantOrganisation();
            final String urn = caseDefendantsWithOrganisation.getUrn();
            final UUID caseId = caseDefendantsWithOrganisation.getCaseId();
            final List<Defendants> defendants = caseDefendantsWithOrganisation.getDefendants();
            final List<String> organisationIds = defendants.stream()
                    .filter(x -> defendantDocument.getDefendants().contains(x.getDefendantId()))
                    .filter(x -> x.getAssociatedOrganisation() != null)
                    .map(x -> x.getAssociatedOrganisation().toString())
                    .distinct()
                    .collect(Collectors.toList());
            sendEmail(jsonEnvelope, materialId, urn, caseId, defendants, organisationIds);
        }
    }

    private void processCaseDocument(final JsonEnvelope jsonEnvelope, final CaseDocument caseDocument, final UUID materialId) {
        if(caseDocument != null) {
            final CaseDefendantsWithOrganisation caseDefendantsWithOrganisation = defenceService.getDefendantsAndAssociatedOrganisationsForCase(jsonEnvelope, caseDocument.getProsecutionCaseId().toString())
                    .getCaseDefendantOrganisation();
            final String urn = caseDefendantsWithOrganisation.getUrn();
            final UUID caseId = caseDefendantsWithOrganisation.getCaseId();
            final List<Defendants> defendants = caseDefendantsWithOrganisation.getDefendants();
            final List<String> organisationIds = defendants.stream()
                    .filter(x -> x.getAssociatedOrganisation() != null)
                    .map(x -> x.getAssociatedOrganisation().toString())
                    .distinct()
                    .collect(Collectors.toList());
            sendEmail(jsonEnvelope, materialId, urn, caseId, defendants, organisationIds);
        }
    }

    private void sendEmail(JsonEnvelope jsonEnvelope, UUID materialId, String urn, UUID caseId, List<Defendants> defendants, List<String> organisationIds) {
        if (!organisationIds.isEmpty() && !defendants.isEmpty()) {
            final Map<String, String> emailForOrganisationIdMap = usersGroupService.getEmailsForOrganisationIds(jsonEnvelope, organisationIds);
            final Map<UUID, String> defendantAndRelatedOrganisationEmail = defendantOrganisationEmailMap(defendants, emailForOrganisationIdMap);
            emailService.sendEmailNotifications(jsonEnvelope, materialId, urn, caseId, defendantAndRelatedOrganisationEmail);
        }
    }

    private Map<UUID, String> defendantOrganisationEmailMap(final List<Defendants> defendants,
                                                           final Map<String, String> emailForOrganisationIdMap) {

        final Map<UUID, String> defendantOrganisationEmailMap = new HashMap<>();
        for (final Defendants defendant : defendants) {
            final UUID associatedOrganisation = defendant.getAssociatedOrganisation();
            if (associatedOrganisation != null && emailForOrganisationIdMap.containsKey(associatedOrganisation.toString())) {
                final String email = emailForOrganisationIdMap.get(associatedOrganisation.toString());
                if (email != null) {
                    defendantOrganisationEmailMap.put(defendant.getDefendantId(), email);
                }
            }
        }
        return defendantOrganisationEmailMap;
    }
}
