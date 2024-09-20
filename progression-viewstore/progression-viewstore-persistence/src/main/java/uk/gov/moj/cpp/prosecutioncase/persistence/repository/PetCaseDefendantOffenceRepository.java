package uk.gov.moj.cpp.prosecutioncase.persistence.repository;


import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PetCaseDefendantOffenceRepository extends EntityRepository<PetCaseDefendantOffence, UUID> {
    List<PetCaseDefendantOffence> findByPetId(final UUID petId);
    List<PetCaseDefendantOffence> findByCaseId(final UUID caseId);
}
