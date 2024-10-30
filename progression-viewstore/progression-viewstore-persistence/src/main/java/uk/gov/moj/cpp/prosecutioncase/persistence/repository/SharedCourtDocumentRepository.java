package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SharedCourtDocumentRepository extends EntityRepository<SharedCourtDocumentEntity, UUID> {

    @Query("from SharedCourtDocumentEntity entity where entity.caseId in (:caseId) and entity.hearingId in (:hearingId) and entity.userGroupId in (:userGroup) and (entity.defendantId is null or entity.defendantId in  (:defendantId)) ORDER BY entity.seqNum ASC")
    List<SharedCourtDocumentEntity> findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(@QueryParam("caseId") UUID caseId, @QueryParam("hearingId") UUID hearingId,
                                                                                             @QueryParam("userGroup") UUID userGroupId, @QueryParam("defendantId") UUID defendantId);
}
