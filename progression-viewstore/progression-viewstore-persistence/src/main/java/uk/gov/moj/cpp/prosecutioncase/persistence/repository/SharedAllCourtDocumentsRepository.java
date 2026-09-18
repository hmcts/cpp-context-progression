package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedAllCourtDocumentsEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SharedAllCourtDocumentsRepository
        extends JpaEntityRepository<SharedAllCourtDocumentsEntity, UUID> {

    public SharedAllCourtDocumentsRepository() {
        super(SharedAllCourtDocumentsEntity.class);
    }

    @Override
    protected UUID idOf(final SharedAllCourtDocumentsEntity entity) {
        return entity.getId();
    }

    public List<SharedAllCourtDocumentsEntity> findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(
            final UUID caseId, final UUID hearingId, final UUID defendantId,
            final List<UUID> userGroupIds, final UUID userId) {
        return entityManager.createQuery(
                        "select entity from SharedAllCourtDocumentsEntity entity"
                                + " where entity.caseId = :caseId"
                                + " and entity.applicationHearingId = :hearingId"
                                + " and entity.defendantId = :defendantId"
                                + " and (entity.userGroupId in (:userGroupIds) or entity.userId = :userId)",
                        SharedAllCourtDocumentsEntity.class)
                .setParameter("caseId", caseId)
                .setParameter("hearingId", hearingId)
                .setParameter("defendantId", defendantId)
                .setParameter("userGroupIds", userGroupIds)
                .setParameter("userId", userId)
                .getResultList();
    }
}
