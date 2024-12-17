package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Repository
public interface CaseProgressionDetailRepository extends EntityRepository<CaseProgressionDetail, UUID> {

    @Override
    List<CaseProgressionDetail> findAll();

    CaseProgressionDetail findByCaseId(UUID caseId);

    @Query(value = "from CaseProgressionDetail c where c.status IN  (?1) ")
    List<CaseProgressionDetail> findByStatus(List<CaseStatusEnum> status);

    @Query(value = "from CaseProgressionDetail c where c.status IN  (?1) and c.caseId = (?2) ")
    List<CaseProgressionDetail> findByStatusAndCaseID(List<CaseStatusEnum> status,  final UUID caseId);

    @Query(value = "from CaseProgressionDetail c where c.status <> 'COMPLETED' ")
    List<CaseProgressionDetail> findOpenStatus();

    @Query(value = "SELECT cd.defendants FROM CaseProgressionDetail cd where cd.caseId = :caseId")
    List<Defendant> findCaseDefendants(@QueryParam("caseId") final UUID caseId);

    @Query(isNative = true, value = "select * from CaseProgressionDetail caseprog where \n" +
            "caseprog.caseid=(select def.caseid from Defendant def inner join defendant_bail_document baildoc on def.defendant_id=baildoc.defendant_id where caseprog.caseid=def.caseid and baildoc.document_id=:materialId) \n")
    public abstract CaseProgressionDetail findByMaterialId(@QueryParam("materialId") final UUID materialId);

}
