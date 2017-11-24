package uk.gov.moj.cpp.progression.persistence.repository;

import org.apache.deltaspike.data.api.QueryParam;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

@Repository
public interface CaseProgressionDetailRepository extends EntityRepository<CaseProgressionDetail, UUID> {

    @Override
    List<CaseProgressionDetail> findAll();

    CaseProgressionDetail findByCaseId(UUID caseId);

    @Query(value = "from CaseProgressionDetail c where c.status IN  (?1) ")
    List<CaseProgressionDetail> findByStatus(List<CaseStatusEnum> status);

    @Query(value = "from CaseProgressionDetail c where c.status IN  (?1) and c.caseId = (?2) ")
    List<CaseProgressionDetail> findByStatusAndCaseID(List<CaseStatusEnum> status,  final UUID caseId);

    @Query(value = "from CaseProgressionDetail c where c.status <> 'COMPLETED') ")
    List<CaseProgressionDetail> findOpenStatus();

    @Query(value = "SELECT cd.defendants FROM CaseProgressionDetail cd where cd.caseId = :caseId")
    List<Defendant> findCaseDefendants(@QueryParam("caseId") final UUID caseId);

    @Query(value = "FROM CaseProgressionDetail cd WHERE UPPER(cd.caseUrn) = UPPER(?1)")
    List<CaseProgressionDetail> findCaseByCaseUrn( String caseUrn);
}
