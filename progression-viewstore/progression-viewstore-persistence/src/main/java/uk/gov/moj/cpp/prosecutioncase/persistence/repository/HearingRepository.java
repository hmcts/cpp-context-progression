package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface HearingRepository extends EntityRepository<HearingEntity, UUID> {

    @Query("from HearingEntity h where h.hearingId in (:hearingIds)")
     List<HearingEntity> findByHearingIds(@QueryParam("hearingIds") List<UUID> hearingIds);

    @Modifying
    @Query("delete from HearingEntity entity where entity.hearingId = :hearingId")
    void removeByHearingId(@QueryParam("hearingId") UUID hearingId);
}
