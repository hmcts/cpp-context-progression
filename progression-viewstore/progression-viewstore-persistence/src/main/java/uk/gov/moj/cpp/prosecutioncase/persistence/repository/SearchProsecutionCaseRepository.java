package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The ordering clauses are kept exactly as they were, including NULLS LAST and the
 * firstName || lastName concatenation, because they determine the order search results are shown in.
 * The unqualified property names the DeltaSpike queries relied on are qualified with the alias, as
 * Hibernate 7 resolves paths rather than bare identifiers.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@ApplicationScoped
public class SearchProsecutionCaseRepository extends JpaEntityRepository<SearchProsecutionCaseEntity, UUID> {

    private static final String ORDER_BY =
            " order by sc.isStandaloneApplication,"
                    + " (sc.defendantFirstName || sc.defendantLastName) NULLS LAST,"
                    + " (sc.defendantFirstName || sc.defendantLastName), sc.prosecutor";

    public SearchProsecutionCaseRepository() {
        super(SearchProsecutionCaseEntity.class);
    }

    @Override
    protected UUID idOf(final SearchProsecutionCaseEntity entity) {
        return entity.getDefendantId();
    }

    public List<SearchProsecutionCaseEntity> findBySearchCriteria(final String searchCriteria) {
        return entityManager.createQuery(
                        "select sc from SearchProsecutionCaseEntity sc"
                                + " where LOWER(sc.searchTarget) LIKE :searchCriteria" + ORDER_BY,
                        SearchProsecutionCaseEntity.class)
                .setParameter("searchCriteria", searchCriteria)
                .getResultList();
    }

    public List<SearchProsecutionCaseEntity> findByCaseId(final String caseId) {
        return entityManager.createQuery(
                        "select sc from SearchProsecutionCaseEntity sc where sc.caseId = :caseId",
                        SearchProsecutionCaseEntity.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<SearchProsecutionCaseEntity> findByCaseUrn(final String caseUrn) {
        return entityManager.createQuery(
                        "select sc from SearchProsecutionCaseEntity sc"
                                + " where sc.reference = :caseUrn" + ORDER_BY,
                        SearchProsecutionCaseEntity.class)
                .setParameter("caseUrn", caseUrn)
                .getResultList();
    }
}
