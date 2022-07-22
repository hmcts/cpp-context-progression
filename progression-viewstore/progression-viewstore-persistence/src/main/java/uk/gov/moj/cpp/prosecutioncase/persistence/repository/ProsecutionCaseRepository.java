package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProsecutionCaseRepository extends EntityRepository<ProsecutionCaseEntity, UUID> {

    @Override
    ProsecutionCaseEntity findBy(UUID id);

    ProsecutionCaseEntity findByCaseId(UUID id);

    ProsecutionCaseEntity findOptionalByCaseId(UUID id);

    @Query("FROM ProsecutionCaseEntity pce where pce.caseId in (:caseIds)")
    List<ProsecutionCaseEntity> findByProsecutionCaseIds(
            @QueryParam("caseIds") final List<UUID> caseIds);

}
