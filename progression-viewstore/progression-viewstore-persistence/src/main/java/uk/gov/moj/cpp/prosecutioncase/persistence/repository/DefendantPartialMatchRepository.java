package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The ordered finders previously returned DeltaSpike's QueryResult, and the query view paged it with
 * {@code .withPageSize(size).toPage(page).getResultList()}. There is no JPA equivalent of that
 * wrapper, so the paging moves onto the query itself: the caller passes the same page size and
 * zero-based page number it was passing to toPage.
 */
@ApplicationScoped
public class DefendantPartialMatchRepository extends JpaEntityRepository<DefendantPartialMatchEntity, UUID> {

    public DefendantPartialMatchRepository() {
        super(DefendantPartialMatchEntity.class);
    }

    @Override
    protected UUID idOf(final DefendantPartialMatchEntity entity) {
        return entity.getDefendantId();
    }

    /**
     * Declared SingleResultType.OPTIONAL, so null rather than NoResultException when absent.
     */
    public DefendantPartialMatchEntity findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery(
                        "select e from DefendantPartialMatchEntity e where e.defendantId = :defendantId",
                        DefendantPartialMatchEntity.class)
                .setParameter("defendantId", defendantId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Derived finder with no SingleResultType, so it keeps the JPA default and throws
     * NoResultException when absent.
     */
    public DefendantPartialMatchEntity findByProsecutionCaseId(final UUID prosecutionCaseId) {
        return entityManager.createQuery(
                        "select e from DefendantPartialMatchEntity e"
                                + " where e.prosecutionCaseId = :prosecutionCaseId",
                        DefendantPartialMatchEntity.class)
                .setParameter("prosecutionCaseId", prosecutionCaseId)
                .getSingleResult();
    }

    public DefendantPartialMatchEntity findByCaseReference(final String caseReference) {
        return entityManager.createQuery(
                        "select e from DefendantPartialMatchEntity e where e.caseReference = :caseReference",
                        DefendantPartialMatchEntity.class)
                .setParameter("caseReference", caseReference)
                .getSingleResult();
    }

    public List<DefendantPartialMatchEntity> findAllOrderByDefendantNameAsc(final int pageSize,
                                                                           final int zeroBasedPage) {
        return pageOf("select d from DefendantPartialMatchEntity d order by d.defendantName asc",
                pageSize, zeroBasedPage);
    }

    public List<DefendantPartialMatchEntity> findAllOrderByDefendantNameDesc(final int pageSize,
                                                                            final int zeroBasedPage) {
        return pageOf("select d from DefendantPartialMatchEntity d order by d.defendantName desc",
                pageSize, zeroBasedPage);
    }

    public List<DefendantPartialMatchEntity> findAllOrderByCaseReceivedDatetimeAsc(final int pageSize,
                                                                                  final int zeroBasedPage) {
        return pageOf("select d from DefendantPartialMatchEntity d order by d.caseReceivedDatetime asc",
                pageSize, zeroBasedPage);
    }

    public List<DefendantPartialMatchEntity> findAllOrderByCaseReceivedDatetimeDesc(final int pageSize,
                                                                                   final int zeroBasedPage) {
        return pageOf("select d from DefendantPartialMatchEntity d order by d.caseReceivedDatetime desc",
                pageSize, zeroBasedPage);
    }

    private List<DefendantPartialMatchEntity> pageOf(final String jpql,
                                                     final int pageSize,
                                                     final int zeroBasedPage) {
        return entityManager.createQuery(jpql, DefendantPartialMatchEntity.class)
                .setFirstResult(zeroBasedPage * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }
}
