package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CourtApplicationCaseRepository
        extends JpaEntityRepository<CourtApplicationCaseEntity, CourtApplicationCaseKey> {

    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";

    private static final String SELECT_ENTITY = "select entity from CourtApplicationCaseEntity entity";

    public CourtApplicationCaseRepository() {
        super(CourtApplicationCaseEntity.class);
    }

    @Override
    protected CourtApplicationCaseKey idOf(final CourtApplicationCaseEntity entity) {
        return entity.getId();
    }

    public List<CourtApplicationCaseEntity> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select entity from CourtApplicationCaseEntity entity where entity.id.caseId in (:caseId)",
                        CourtApplicationCaseEntity.class)
                .setParameter(CASE_ID, caseId)
                .getResultList();
    }

    public List<CourtApplicationCaseEntity> findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery(
                        SELECT_ENTITY
                                + " where entity.id.applicationId in (:applicationId)",
                        CourtApplicationCaseEntity.class)
                .setParameter(APPLICATION_ID, applicationId)
                .getResultList();
    }

    /**
     * Projects a single scalar column, so it is typed String rather than the entity.
     */
    public String findCaseStatusByApplicationId(final UUID applicationId, final UUID caseId) {
        return entityManager.createQuery(
                        "SELECT pc.payload FROM ProsecutionCaseEntity pc, CourtApplicationCaseEntity cac"
                                + " WHERE cac.id.applicationId = :applicationId AND cac.id.caseId = :caseId"
                                + " AND pc.caseId = cac.id.caseId",
                        String.class)
                .setParameter(APPLICATION_ID, applicationId)
                .setParameter(CASE_ID, caseId)
                .getSingleResult();
    }

    public void removeByApplicationId(final UUID applicationId) {
        entityManager.createQuery(
                        "delete from CourtApplicationCaseEntity entity"
                                + " where entity.id.applicationId = :applicationId")
                .setParameter(APPLICATION_ID, applicationId)
                .executeUpdate();
    }

    /**
     * Declared SingleResultType.OPTIONAL, so this one returns null rather than throwing when there
     * is no match - unlike the derived finders, which keep the JPA default.
     */
    public CourtApplicationCaseEntity findByApplicationIdAndCaseId(final UUID applicationId, final UUID caseId) {
        return entityManager.createQuery(
                        SELECT_ENTITY
                                + " WHERE entity.id.applicationId = :applicationId AND entity.id.caseId = :caseId",
                        CourtApplicationCaseEntity.class)
                .setParameter(APPLICATION_ID, applicationId)
                .setParameter(CASE_ID, caseId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
