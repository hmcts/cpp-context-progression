package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;
import java.util.UUID;

@Repository
public interface DefendantPartialMatchRepository extends EntityRepository<DefendantPartialMatchEntity, UUID> {

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
