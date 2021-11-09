package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;

@Repository
public interface DefendantPartialMatchRepository extends EntityRepository<DefendantPartialMatchEntity, UUID> {

    @Query(singleResult = SingleResultType.OPTIONAL)
    DefendantPartialMatchEntity findByDefendantId(UUID defendantId);

    DefendantPartialMatchEntity findByProsecutionCaseId(UUID prosecutionCaseId);

    DefendantPartialMatchEntity findByCaseReference(String caseReference);

    @Query("select d from DefendantPartialMatchEntity d order by d.defendantName asc")
    QueryResult<DefendantPartialMatchEntity> findAllOrderByDefendantNameAsc();

    @Query("select d from DefendantPartialMatchEntity d order by d.defendantName desc")
    QueryResult<DefendantPartialMatchEntity> findAllOrderByDefendantNameDesc();

    @Query("select d from DefendantPartialMatchEntity d order by d.caseReceivedDatetime asc")
    QueryResult<DefendantPartialMatchEntity> findAllOrderByCaseReceivedDatetimeAsc();

    @Query("select d from DefendantPartialMatchEntity d order by d.caseReceivedDatetime desc")
    QueryResult<DefendantPartialMatchEntity> findAllOrderByCaseReceivedDatetimeDesc();
}
