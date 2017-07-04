package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseProgressionDetailRepository extends EntityRepository<CaseProgressionDetail, UUID> {

    @Override
    List<CaseProgressionDetail> findAll();

    CaseProgressionDetail findByCaseId(UUID caseId);

    @Query(value = "from CaseProgressionDetail c where c.status IN  (?1) ")
    List<CaseProgressionDetail> findByStatus(List<CaseStatusEnum> status);

    @Query(value = "from CaseProgressionDetail c where c.status <> 'COMPLETED') ")
    List<CaseProgressionDetail> findOpenStatus();

}
