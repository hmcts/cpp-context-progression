package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseLinkSplitMergeRepository extends EntityRepository<CaseLinkSplitMergeEntity, UUID> {

    List<CaseLinkSplitMergeEntity> findByLinkGroupId(UUID linkGroupId);

    List<CaseLinkSplitMergeEntity> findByCaseId(UUID caseId);

    List<CaseLinkSplitMergeEntity> findByCaseIdAndLinkedCaseIdAndType(UUID caseId, UUID linkedCaseId, LinkType type);

    @Query("from CaseLinkSplitMergeEntity entity where entity.caseId != :caseId and entity.reference = :reference and entity.type = 'MERGE'")
    public abstract List<CaseLinkSplitMergeEntity> findPreviousMergesByReference(@QueryParam("caseId") final UUID caseId,
                                                                                 @QueryParam("reference") final String reference);
}
