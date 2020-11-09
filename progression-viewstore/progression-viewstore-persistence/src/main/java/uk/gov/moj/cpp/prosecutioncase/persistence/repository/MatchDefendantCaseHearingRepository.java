package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchDefendantCaseHearingRepository extends EntityRepository<MatchDefendantCaseHearingEntity, UUID> {

    List<MatchDefendantCaseHearingEntity> findByMasterDefendantId(UUID masterDefendantId);

    List<MatchDefendantCaseHearingEntity> findByProsecutionCaseIdAndDefendantId(UUID prosecutionCaseId, UUID defendantId);

    @Query
    List<MatchDefendantCaseHearingEntity> findByDefendantId(UUID defendantId);

    @Query(singleResult = SingleResultType.OPTIONAL)
    MatchDefendantCaseHearingEntity findByHearingIdAndProsecutionCaseIdAndDefendantId(UUID hearingId, UUID prosecutionCaseId, UUID defendantId);

    @Query(value = "from MatchDefendantCaseHearingEntity m where m.masterDefendantId IN  (?1) ")
    List<MatchDefendantCaseHearingEntity> findByMasterDefendantId(List<UUID> masterDefendantId);
}
