package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PublicListOpaNotice;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PublicListOpaNoticeRepository extends JpaEntityRepository<PublicListOpaNotice, UUID> {

    public PublicListOpaNoticeRepository() {
        super(PublicListOpaNotice.class);
    }

    @Override
    protected UUID idOf(final PublicListOpaNotice entity) {
        return entity.getDefendantId();
    }

    /**
     * Derived delete: DeltaSpike generated "delete from PublicListOpaNotice where defendantId = :defendantId".
     */
    public void deleteByDefendantId(final UUID defendantId) {
        entityManager.createQuery(
                        "delete from PublicListOpaNotice e where e.defendantId = :defendantId")
                .setParameter("defendantId", defendantId)
                .executeUpdate();
    }
}
