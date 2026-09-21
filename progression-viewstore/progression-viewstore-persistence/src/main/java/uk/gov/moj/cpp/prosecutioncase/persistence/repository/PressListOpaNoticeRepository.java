package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PressListOpaNotice;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PressListOpaNoticeRepository extends JpaEntityRepository<PressListOpaNotice, UUID> {

    public PressListOpaNoticeRepository() {
        super(PressListOpaNotice.class);
    }

    @Override
    protected UUID idOf(final PressListOpaNotice entity) {
        return entity.getDefendantId();
    }

    /**
     * Derived delete: DeltaSpike generated "delete from PressListOpaNotice where defendantId = :defendantId".
     */
    public void deleteByDefendantId(final UUID defendantId) {
        entityManager.createQuery(
                        "delete from PressListOpaNotice e where e.defendantId = :defendantId")
                .setParameter("defendantId", defendantId)
                .executeUpdate();
    }
}
