package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HearingRepository extends JpaEntityRepository<HearingEntity, UUID> {

    public HearingRepository() {
        super(HearingEntity.class);
    }

    @Override
    protected UUID idOf(final HearingEntity entity) {
        return entity.getHearingId();
    }

    public List<HearingEntity> findByHearingIds(final List<UUID> hearingIds) {
        return entityManager.createQuery(
                        "select h from HearingEntity h where h.hearingId in (:hearingIds)", HearingEntity.class)
                .setParameter("hearingIds", hearingIds)
                .getResultList();
    }

    public void removeByHearingId(final UUID hearingId) {
        entityManager.createQuery(
                        "delete from HearingEntity entity where entity.hearingId = :hearingId")
                .setParameter("hearingId", hearingId)
                .executeUpdate();
    }
}
