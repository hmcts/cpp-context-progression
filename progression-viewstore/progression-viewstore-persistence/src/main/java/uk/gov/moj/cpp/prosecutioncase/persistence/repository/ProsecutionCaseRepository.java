package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ProsecutionCaseRepository extends EntityRepository<ProsecutionCaseEntity, UUID> {

    @Override
    ProsecutionCaseEntity findBy(UUID id);

    ProsecutionCaseEntity findByCaseId(UUID id);

    ProsecutionCaseEntity findOptionalByCaseId(UUID id);

    List<ProsecutionCaseEntity> findByGroupId(final UUID groupId);

    @Query("FROM ProsecutionCaseEntity pce where pce.caseId in (:caseIds)")
    List<ProsecutionCaseEntity> findByProsecutionCaseIds(
            @QueryParam("caseIds") final List<UUID> caseIds);

}
