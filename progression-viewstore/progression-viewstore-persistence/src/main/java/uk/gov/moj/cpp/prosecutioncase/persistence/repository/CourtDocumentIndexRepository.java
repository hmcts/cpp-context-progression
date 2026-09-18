package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CourtDocumentIndexRepository extends JpaEntityRepository<CourtDocumentIndexEntity, UUID> {

    public CourtDocumentIndexRepository() {
        super(CourtDocumentIndexEntity.class);
    }

    @Override
    protected UUID idOf(final CourtDocumentIndexEntity entity) {
        return entity.getId();
    }

    /**
     * Declared SingleResultType.OPTIONAL, so null rather than NoResultException when absent.
     */
    public CourtDocumentIndexEntity findByCaseIdDefendantIdAndCaseDocumentId(final UUID caseId,
                                                                            final UUID defendantId,
                                                                            final UUID courtDocumentId) {
        return entityManager.createQuery(
                        "select cdie from CourtDocumentIndexEntity cdie"
                                + " where cdie.prosecutionCaseId = :caseId and cdie.defendantId = :defendantId"
                                + " and cdie.courtDocument.courtDocumentId = :courtDocumentId",
                        CourtDocumentIndexEntity.class)
                .setParameter("caseId", caseId)
                .setParameter("defendantId", defendantId)
                .setParameter("courtDocumentId", courtDocumentId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<CourtDocumentIndexEntity> findByMaterialId(final UUID materialId) {
        return entityManager.createQuery(
                        "select di from CourtDocumentIndexEntity di, CourtDocumentMaterialEntity dm"
                                + " where dm.materialId = :materialId"
                                + " and dm.courtDocumentId = di.courtDocument.courtDocumentId",
                        CourtDocumentIndexEntity.class)
                .setParameter("materialId", materialId)
                .getResultList();
    }

    public void updateApplicationIdByApplicationId(final UUID newApplicationId, final UUID applicationId) {
        entityManager.createQuery(
                        "update CourtDocumentIndexEntity cdie set cdie.applicationId = :newApplicationId"
                                + " where cdie.applicationId = :applicationId")
                .setParameter("newApplicationId", newApplicationId)
                .setParameter("applicationId", applicationId)
                .executeUpdate();
    }

    public List<CourtDocumentIndexEntity> findByProsecutionCaseIdAndDefendantId(final UUID prosecutionCaseId,
                                                                               final UUID defendantId) {
        return entityManager.createQuery(
                        "select e from CourtDocumentIndexEntity e"
                                + " where e.prosecutionCaseId = :prosecutionCaseId and e.defendantId = :defendantId",
                        CourtDocumentIndexEntity.class)
                .setParameter("prosecutionCaseId", prosecutionCaseId)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }
}
