package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseDefendantHearingRepository extends EntityRepository<CaseDefendantHearingEntity, CaseDefendantHearingKey> {

    @Query("from CaseDefendantHearingEntity entity where entity.id.caseId in (:caseId)")
    public abstract List<CaseDefendantHearingEntity> findByCaseId(@QueryParam("caseId") UUID caseId);

    @Query("from CaseDefendantHearingEntity entity where entity.id.hearingId in (:hearingId)")
    public abstract List<CaseDefendantHearingEntity> findByHearingId(@QueryParam("hearingId") UUID hearingId);

    @Query("from CaseDefendantHearingEntity entity where entity.id.defendantId in (:defendantId)")
    public abstract List<CaseDefendantHearingEntity> findByDefendantId(@QueryParam("defendantId") UUID defendantId);

    @Query("from CaseDefendantHearingEntity entity where entity.id.defendantId in (:defendantId) and entity.id.caseId in (:caseId)")
    public abstract List<CaseDefendantHearingEntity> findByCaseIdAndDefendantId(@QueryParam("caseId") UUID caseId,
                                                                                @QueryParam("defendantId") UUID defendantId);

    @Query("from CaseDefendantHearingEntity entity where entity.id.hearingId in (:hearingId) and entity.id.caseId in (:caseId) and entity.id.defendantId in (:defendantId)")
    public abstract CaseDefendantHearingEntity findByHearingIdAndCaseIdAndDefendantId(@QueryParam("hearingId") UUID hearingId,
                                                                                      @QueryParam("caseId") UUID caseId,
                                                                                      @QueryParam("defendantId") UUID defendantId);
    @Modifying
    @Query("delete from CaseDefendantHearingEntity entity where entity.id.hearingId in (:hearingId) and entity.id.caseId in (:caseId) and entity.id.defendantId in (:defendantId)")
    void removeByHearingIdAndCaseIdAndDefendantId(@QueryParam("hearingId") UUID hearingId,
                                                  @QueryParam("caseId") UUID caseId,
                                                  @QueryParam("defendantId") UUID defendantId);

    @Modifying
    @Query("delete from CaseDefendantHearingEntity entity where entity.id.hearingId in (:hearingId) and entity.id.caseId in (:caseId)")
    void removeByHearingIdAndCaseId(@QueryParam("hearingId") UUID hearingId,
                                    @QueryParam("caseId") UUID caseId);

    @Modifying
    @Query("delete from CaseDefendantHearingEntity entity where entity.id.hearingId in (:hearingId) and entity.id.defendantId in (:defendantId)")
    void removeByHearingIdAndDefendantId(@QueryParam("hearingId") UUID hearingId,
                                    @QueryParam("defendantId") UUID defendantId);

    @Modifying
    @Query("delete from CaseDefendantHearingEntity entity where entity.id.hearingId in (:hearingId)" )
    void removeByHearingId(@QueryParam("hearingId") UUID hearingId);


}
