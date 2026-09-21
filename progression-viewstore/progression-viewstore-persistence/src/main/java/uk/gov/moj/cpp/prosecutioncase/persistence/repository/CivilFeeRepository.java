package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CivilFeeRepository extends JpaEntityRepository<CivilFeeEntity, UUID> {

    public CivilFeeRepository() {
        super(CivilFeeEntity.class);
    }

    @Override
    protected UUID idOf(final CivilFeeEntity entity) {
        return entity.getFeeId();
    }

    public List<CivilFeeEntity> findByFeeIds(final List<UUID> feeIds) {
        return entityManager.createQuery(
                        "select c from CivilFeeEntity c where c.feeId in (:feeIds)", CivilFeeEntity.class)
                .setParameter("feeIds", feeIds)
                .getResultList();
    }
}
