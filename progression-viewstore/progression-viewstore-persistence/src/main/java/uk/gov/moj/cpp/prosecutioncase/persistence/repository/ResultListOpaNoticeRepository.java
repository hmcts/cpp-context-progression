package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ResultListOpaNotice;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResultListOpaNoticeRepository extends JpaEntityRepository<ResultListOpaNotice, UUID> {

    public ResultListOpaNoticeRepository() {
        super(ResultListOpaNotice.class);
    }

    @Override
    protected UUID idOf(final ResultListOpaNotice entity) {
        return entity.getDefendantId();
    }

    /**
     * Derived delete: DeltaSpike generated "delete from ResultListOpaNotice where defendantId = :defendantId".
     */
    public void deleteByDefendantId(final UUID defendantId) {
        entityManager.createQuery(
                        "delete from ResultListOpaNotice e where e.defendantId = :defendantId")
                .setParameter("defendantId", defendantId)
                .executeUpdate();
    }
}
