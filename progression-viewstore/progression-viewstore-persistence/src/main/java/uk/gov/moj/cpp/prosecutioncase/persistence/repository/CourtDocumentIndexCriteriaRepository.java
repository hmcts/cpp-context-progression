package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
@Repository(forEntity = CourtDocumentIndexEntity.class)
public abstract class CourtDocumentIndexCriteriaRepository implements EntityRepository<CourtDocumentIndexEntity, UUID>, CriteriaSupport<CourtDocumentIndexEntity> {

    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String NAME = "name";

    @Inject
    private EntityManager entityManager;

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
