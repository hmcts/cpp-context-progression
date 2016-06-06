package uk.gov.moj.progression.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;

@Repository
public interface IndicateStatementRepository extends EntityRepository<IndicateStatement, UUID> {

    @Override
    List<IndicateStatement> findAll();

    List<IndicateStatement> findByCaseId(UUID caseId);

    IndicateStatement findById(UUID Id);
}
