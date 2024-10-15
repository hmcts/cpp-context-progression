package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CourtApplicationCaseRepository extends EntityRepository<CourtApplicationCaseEntity, CourtApplicationCaseKey> {

    @Query("from CourtApplicationCaseEntity entity where entity.id.caseId in (:caseId)")
    public abstract List<CourtApplicationCaseEntity> findByCaseId(@QueryParam("caseId") UUID caseId);

    @Query("from CourtApplicationCaseEntity entity where entity.id.applicationId in (:applicationId)")
    public abstract List<CourtApplicationCaseEntity> findByApplicationId(@QueryParam("applicationId") UUID applicationId);

    @Query(value = "SELECT pc.payload FROM ProsecutionCaseEntity pc,CourtApplicationCaseEntity cac WHERE cac.id.applicationId = :applicationId AND cac.id.caseId = :caseId AND pc.caseId = cac.id.caseId")
    String findCaseStatusByApplicationId(@QueryParam("applicationId") final UUID applicationId, @QueryParam("caseId") final UUID caseId);

    @Modifying
    @Query("delete from CourtApplicationCaseEntity entity where entity.id.applicationId = :applicationId")
    void removeByApplicationId(@QueryParam("applicationId") UUID applicationId);
}
