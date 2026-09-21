package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Was an abstract DeltaSpike repository implementing EntityRepository and CriteriaSupport, though it
 * only ever used the EntityManager directly for its criteria queries.
 *
 * The EntityManager is now inherited from JpaEntityRepository, which injects it with
 * {@code @PersistenceContext} rather than {@code @Inject}. That matters: on WildFly 40, an
 * {@code @Inject EntityManager} resolves to Weld's @TransactionScoped producer, and the query
 * dispatch path is not transactional - calling getCriteriaBuilder() there throws
 * ContextNotActiveException (WELD-001303) and the query returns HTTP 500. Deploy and unit tests
 * both pass; only a running query shows it.
 */
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
@ApplicationScoped
public class CourtDocumentIndexCriteriaRepository
        extends JpaEntityRepository<CourtDocumentIndexEntity, UUID> {

    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String NAME = "name";

    public CourtDocumentIndexCriteriaRepository() {
        super(CourtDocumentIndexEntity.class);
    }

    @Override
    protected UUID idOf(final CourtDocumentIndexEntity entity) {
        return entity.getId();
    }

    public Long countByCriteria(final SearchCriteria searchCriteria) {
        final CriteriaBuilder cBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> cQuery = cBuilder.createQuery(Long.class);
        final Root<CourtDocumentIndexEntity> courtDocumentEntityRoot = cQuery.from(CourtDocumentIndexEntity.class);
        final List<Predicate> predicates = getAllPredicates(searchCriteria, cBuilder, courtDocumentEntityRoot);
        cQuery.select(cBuilder.count(courtDocumentEntityRoot));
        cQuery.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(cQuery).getSingleResult();
    }

    public List<CourtDocumentIndexEntity> getCourtDocumentIndexByCriteria(final SearchCriteria searchCriteria) {
        final CriteriaBuilder cBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<CourtDocumentIndexEntity> cQuery = cBuilder.createQuery(CourtDocumentIndexEntity.class);
        final Root<CourtDocumentIndexEntity> courtDocumentEntityRoot = cQuery.from(CourtDocumentIndexEntity.class);
        final List<Predicate> predicates = getAllPredicates(searchCriteria, cBuilder, courtDocumentEntityRoot);
        cQuery.where(predicates.toArray(new Predicate[0]));
        final TypedQuery<CourtDocumentIndexEntity> typedQuery = entityManager.createQuery(cQuery);
        return typedQuery.getResultList();
    }

    private List<Predicate> getAllPredicates(final SearchCriteria searchCriteria, final CriteriaBuilder cBuilder, final Root<CourtDocumentIndexEntity> courtDocumentEntityRoot) {
        final List<Predicate> predicates = new ArrayList<>();

        predicates.add(cBuilder.equal(courtDocumentEntityRoot.get(PROSECUTION_CASE_ID), searchCriteria.getCaseId()));
        searchCriteria.getDefendantId().ifPresent(value -> predicates.add(cBuilder.equal(courtDocumentEntityRoot.get("defendantId"), value)));
        predicates.addAll(getCourtDocumentPredicates(cBuilder, searchCriteria, courtDocumentEntityRoot));
        return predicates;
    }

    private List<Predicate> getCourtDocumentPredicates(final CriteriaBuilder cBuilder, final SearchCriteria searchCriteria, final Root<CourtDocumentIndexEntity> courtDocumentIndexEntityRoot) {
        final List<Predicate> predicates = new ArrayList<>();
        final Join<CourtDocumentIndexEntity, CourtDocumentEntity> participantSetJoin = courtDocumentIndexEntityRoot.join("courtDocument");
        predicates.add(cBuilder.equal(participantSetJoin.get("isRemoved"), false));
        searchCriteria.getSectionId().ifPresent(value -> predicates.add(cBuilder.equal(
                cBuilder.function("jsonb_extract_path_text", String.class, cBuilder.function("jsonb", String.class, participantSetJoin.get("payload")), cBuilder.literal("documentTypeId")),
                value
        )));

        return predicates;
    }
}
