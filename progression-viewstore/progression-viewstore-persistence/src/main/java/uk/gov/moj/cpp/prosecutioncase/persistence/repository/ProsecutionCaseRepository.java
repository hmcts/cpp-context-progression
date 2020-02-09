package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;

import java.util.UUID;

@Repository
public interface ProsecutionCaseRepository extends EntityRepository<ProsecutionCaseEntity, UUID> {

    @Override
    ProsecutionCaseEntity findBy(UUID id);

    ProsecutionCaseEntity findByCaseId(UUID id);

}
