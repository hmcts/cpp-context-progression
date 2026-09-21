package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RelatedReferenceRepository extends JpaEntityRepository<RelatedReference, UUID> {

    public RelatedReferenceRepository() {
        super(RelatedReference.class);
    }

    @Override
    protected UUID idOf(final RelatedReference entity) {
        return entity.getId();
    }

    public List<RelatedReference> findByProsecutionCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select e from RelatedReference e where e.prosecutionCaseId = :caseId", RelatedReference.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }
}
