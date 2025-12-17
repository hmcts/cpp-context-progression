package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourtApplicationRepository extends EntityRepository<CourtApplicationEntity, UUID> {

    @Override
    CourtApplicationEntity findBy(UUID id);

    CourtApplicationEntity findByApplicationId(UUID id);

    List<CourtApplicationEntity> findByParentApplicationId(UUID id);

    @Modifying
    @Query("delete from CourtApplicationEntity entity where entity.applicationId = :applicationId")
    void removeByApplicationId(@QueryParam("applicationId") UUID applicationId);

    @Query("from CourtApplicationEntity entity where entity.applicationId in (:applicationIds)")
    List<CourtApplicationEntity> findByApplicationIds(@QueryParam("applicationIds") List<UUID> applicationIds);
}
