package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PetCaseDefendantOffenceRepository extends JpaEntityRepository<PetCaseDefendantOffence, UUID> {

    public PetCaseDefendantOffenceRepository() {
        super(PetCaseDefendantOffence.class);
    }

    @Override
    protected UUID idOf(final PetCaseDefendantOffence entity) {
        return entity.getId();
    }

    public List<PetCaseDefendantOffence> findByPetId(final UUID petId) {
        return entityManager.createQuery(
                        "select e from PetCaseDefendantOffence e where e.petId = :petId", PetCaseDefendantOffence.class)
                .setParameter("petId", petId)
                .getResultList();
    }

    public List<PetCaseDefendantOffence> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select e from PetCaseDefendantOffence e where e.caseId = :caseId", PetCaseDefendantOffence.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }
}
