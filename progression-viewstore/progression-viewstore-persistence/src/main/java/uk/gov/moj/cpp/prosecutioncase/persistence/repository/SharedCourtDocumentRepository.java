package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SharedCourtDocumentRepository extends JpaEntityRepository<SharedCourtDocumentEntity, UUID> {

    public SharedCourtDocumentRepository() {
        super(SharedCourtDocumentEntity.class);
    }

    @Override
    protected UUID idOf(final SharedCourtDocumentEntity entity) {
        return entity.getId();
    }

    public List<SharedCourtDocumentEntity> findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(
            final UUID caseId, final UUID hearingId, final UUID userGroupId, final UUID defendantId) {
        return entityManager.createQuery(
                        "select entity from SharedCourtDocumentEntity entity"
                                + " where entity.caseId in (:caseId) and entity.hearingId in (:hearingId)"
                                + " and entity.userGroupId in (:userGroup)"
                                + " and (entity.defendantId is null or entity.defendantId in (:defendantId))"
                                + " ORDER BY entity.seqNum ASC",
                        SharedCourtDocumentEntity.class)
                .setParameter("caseId", caseId)
                .setParameter("hearingId", hearingId)
                .setParameter("userGroup", userGroupId)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }
}
