package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourtApplicationRepository extends EntityRepository<CourtApplicationEntity, UUID> {

    @Override
    CourtApplicationEntity findBy(UUID id);

    CourtApplicationEntity findByApplicationId(UUID id);

    List<CourtApplicationEntity> findByLinkedCaseId(UUID id);

    List<CourtApplicationEntity> findByParentApplicationId(UUID id);
}
