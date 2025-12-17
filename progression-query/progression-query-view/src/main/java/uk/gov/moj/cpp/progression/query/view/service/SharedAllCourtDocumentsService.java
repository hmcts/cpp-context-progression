package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getDefendantName;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.progression.courts.SharedCourtDocumentsLinksForApplication;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedAllCourtDocumentsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class SharedAllCourtDocumentsService {


    @Inject
    private SharedAllCourtDocumentsRepository sharedAllCourtDocumentsRepository;

    @Inject
    private UserService userService;

    public List<SharedCourtDocumentsLinksForApplication> getSharedAllCourtDocumentsForTrialHearing(final JsonEnvelope envelope, final UUID caseId, final String caseUrn, final List<Defendant> defendants, final UUID applicationHearingId) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new IllegalStateException("UserId missing from query.")));
        final List<UUID> userGroupIds = userService.getUserGroupIdsByUserId(envelope);
        final List<SharedCourtDocumentsLinksForApplication> sharedAllCourtDocuments = new ArrayList<>();
        defendants.forEach(defendant -> {
            final boolean isAllCourtDocumentsShared = isNotEmpty(sharedAllCourtDocumentsRepository.findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(caseId, applicationHearingId, defendant.getMasterDefendantId(), userGroupIds, userId));
            if (isAllCourtDocumentsShared) {
                sharedAllCourtDocuments.add(SharedCourtDocumentsLinksForApplication.sharedCourtDocumentsLinksForApplication()
                        .withCaseId(caseId)
                        .withCaseUrn(caseUrn)
                        .withDefendantId(defendant.getMasterDefendantId())
                        .withDefendantName(getDefendantName(defendant.getPersonDefendant(), defendant.getLegalEntityDefendant()))
                        .build());
            }
        });

        return sharedAllCourtDocuments;
    }

    public List<SharedCourtDocumentsLinksForApplication> getSharedAllCourtDocuments(final UUID caseId, final String caseUrn, final List<Defendant> defendants) {
        final List<SharedCourtDocumentsLinksForApplication> sharedCourtDocumentsLinks = new ArrayList<>();
        defendants.forEach(d -> sharedCourtDocumentsLinks.add(SharedCourtDocumentsLinksForApplication.sharedCourtDocumentsLinksForApplication()
                .withDefendantName(getDefendantName(d.getPersonDefendant(), d.getLegalEntityDefendant()))
                .withDefendantId(d.getId())
                .withCaseId(caseId)
                .withCaseUrn(caseUrn)
                .build()));

        return sharedCourtDocumentsLinks;

    }

}
