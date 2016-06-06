package uk.gov.moj.progression.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

@Repository
public interface CaseProgressionDetailRepository extends EntityRepository<CaseProgressionDetail, UUID> {

    @Override
    List<CaseProgressionDetail> findAll();

    CaseProgressionDetail findByCaseId(UUID caseId);

    CaseProgressionDetail findById(UUID id);
}
