package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.justice.core.courts.FormType;
import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CaseDefendantOffenceRepository extends JpaEntityRepository<CaseDefendantOffence, UUID> {

    public CaseDefendantOffenceRepository() {
        super(CaseDefendantOffence.class);
    }

    @Override
    protected UUID idOf(final CaseDefendantOffence entity) {
        return entity.getId();
    }

    /**
     * Returns a list rather than the single entity {@code findBy(id)} gives, which is why it
     * coexists with the inherited built-in.
     */
    public List<CaseDefendantOffence> findById(final UUID id) {
        return entityManager.createQuery(
                        "select e from CaseDefendantOffence e where e.id = :id", CaseDefendantOffence.class)
                .setParameter("id", id)
                .getResultList();
    }

    public List<CaseDefendantOffence> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select e from CaseDefendantOffence e where e.caseId = :caseId", CaseDefendantOffence.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<CaseDefendantOffence> findByCaseIdAndFormType(final UUID caseId, final FormType formType) {
        return entityManager.createQuery(
                        "select e from CaseDefendantOffence e where e.caseId = :caseId and e.formType = :formType",
                        CaseDefendantOffence.class)
                .setParameter("caseId", caseId)
                .setParameter("formType", formType)
                .getResultList();
    }

    public List<CaseDefendantOffence> findByCourtFormId(final UUID courtFormId) {
        return entityManager.createQuery(
                        "select e from CaseDefendantOffence e where e.courtFormId = :courtFormId",
                        CaseDefendantOffence.class)
                .setParameter("courtFormId", courtFormId)
                .getResultList();
    }
}
