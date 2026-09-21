package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CourtApplicationRepository extends JpaEntityRepository<CourtApplicationEntity, UUID> {

    public CourtApplicationRepository() {
        super(CourtApplicationEntity.class);
    }

    @Override
    protected UUID idOf(final CourtApplicationEntity entity) {
        return entity.getApplicationId();
    }

    /**
     * Throws NoResultException when there is no match, which is what the DeltaSpike derived finder
     * did (its default is SingleResultType.JPA). ApplicationQueryView catches that exception and
     * relies on it - returning null here instead would turn a handled "not found" into an NPE.
     */
    public CourtApplicationEntity findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery(
                        "select entity from CourtApplicationEntity entity where entity.applicationId = :applicationId",
                        CourtApplicationEntity.class)
                .setParameter("applicationId", applicationId)
                .getSingleResult();
    }

    public List<CourtApplicationEntity> findByParentApplicationId(final UUID parentApplicationId) {
        return entityManager.createQuery(
                        "select entity from CourtApplicationEntity entity where entity.parentApplicationId = :parentApplicationId",
                        CourtApplicationEntity.class)
                .setParameter("parentApplicationId", parentApplicationId)
                .getResultList();
    }

    public void removeByApplicationId(final UUID applicationId) {
        entityManager.createQuery(
                        "delete from CourtApplicationEntity entity where entity.applicationId = :applicationId")
                .setParameter("applicationId", applicationId)
                .executeUpdate();
    }

    public List<CourtApplicationEntity> findByApplicationIds(final List<UUID> applicationIds) {
        return entityManager.createQuery(
                        "select entity from CourtApplicationEntity entity where entity.applicationId in (:applicationIds)",
                        CourtApplicationEntity.class)
                .setParameter("applicationIds", applicationIds)
                .getResultList();
    }
}
