package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class RelatedReferenceRepository extends AbstractEntityRepository<RelatedReference, UUID> {

    public List<RelatedReference> findByProsecutionCaseId(final UUID caseId) {
        return entityManager().createQuery(
                        "select reference from RelatedReference reference where reference.prosecutionCaseId = :caseId",
                        RelatedReference.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

}
