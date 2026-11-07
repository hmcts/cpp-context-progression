package uk.gov.moj.cpp.progression.pilot;

import static jakarta.ejb.TransactionAttributeType.REQUIRES_NEW;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
@TransactionAttribute(REQUIRES_NEW)
public class RelatedReferencePilotRepository {
    @PersistenceContext(unitName = "progression-pilot")
    private EntityManager entityManager;

    @Resource
    private SessionContext transaction;

    public void save(final RelatedReference reference) {
        entityManager.merge(reference);
    }

    public void saveAndRollBack(final RelatedReference reference) {
        save(reference);
        entityManager.flush();
        transaction.setRollbackOnly();
    }

    public List<RelatedReference> findByProsecutionCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select r from RelatedReference r where r.prosecutionCaseId = :caseId",
                        RelatedReference.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }
}
