package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedAllCourtDocumentsEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SharedAllCourtDocumentsRepository extends EntityRepository<SharedAllCourtDocumentsEntity, UUID> {

    @Query("from SharedAllCourtDocumentsEntity entity where entity.caseId = :caseId and entity.applicationHearingId = :hearingId and entity.defendantId = :defendantId  and (entity.userGroupId in (:userGroupIds) or  userId = :userId) ")
    List<SharedAllCourtDocumentsEntity> findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(@QueryParam("caseId") UUID caseId, @QueryParam("hearingId") UUID hearingId, @QueryParam("defendantId") UUID defendantId,
                                                                                                     @QueryParam("userGroupIds") List<UUID> userGroupIds, @QueryParam("userId") final UUID userId);


}
