package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository for {@link OffenceDetail}
 * @deprecated
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ApplicationScoped
public class OffenceRepository extends JpaEntityRepository<OffenceDetail, UUID> {

    public OffenceRepository() {
        super(OffenceDetail.class);
    }

    @Override
    protected UUID idOf(final OffenceDetail entity) {
        return entity.getId();
    }

    public List<OffenceDetail> findByDefendantOrderByOrderIndex(final Defendant defendant) {
        return entityManager.createQuery(
                        "select o from OffenceDetail o where o.defendant = :defendant order by o.orderIndex",
                        OffenceDetail.class)
                .setParameter("defendant", defendant)
                .getResultList();
    }
}
