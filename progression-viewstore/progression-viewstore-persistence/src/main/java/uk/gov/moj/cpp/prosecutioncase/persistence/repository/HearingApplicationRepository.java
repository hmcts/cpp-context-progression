package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;

import java.util.List;
import java.util.UUID;

@Repository
public interface HearingApplicationRepository extends EntityRepository<HearingApplicationEntity, HearingApplicationKey> {

    @Query("from HearingApplicationEntity entity where entity.id.applicationId in (:applicationId)")
    public abstract List<HearingApplicationEntity> findByApplicationId(@QueryParam("applicationId") UUID applicationId);

    @Query("from HearingApplicationEntity entity where entity.id.hearingId in (:hearingId)")
    public abstract List<HearingApplicationEntity> findByHearingId(@QueryParam("hearingId") UUID hearingId);

    @Modifying
    @Query("delete from HearingApplicationEntity entity where entity.id.hearingId in (:hearingId) and entity.id.applicationId in (:applicationId)")
    void removeByHearingIdAndCourtApplicationId(@QueryParam("hearingId") UUID hearingId,
                                                @QueryParam("applicationId") UUID applicationId);

    @Modifying
    @Query("delete from HearingApplicationEntity entity where entity.id.hearingId in (:hearingId)")
    void removeByHearingId(@QueryParam("hearingId") UUID hearingId);

    @Modifying
    @Query("delete from HearingApplicationEntity entity where entity.id.applicationId = :applicationId")
    void removeByApplicationId(@QueryParam("applicationId") UUID applicationId);
}
