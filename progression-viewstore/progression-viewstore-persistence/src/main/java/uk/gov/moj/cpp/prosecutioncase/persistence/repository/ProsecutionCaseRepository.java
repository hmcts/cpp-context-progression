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

    @Query(value = "SELECT CAST(jsonb_build_object('inactiveCaseSummary', jsonb_build_object( " +
            "    'id', p.id, " +
            "    'migrationSourceSystem', COALESCE(CAST(p.payload AS jsonb) -> 'migrationSourceSystem', CAST('{}' AS jsonb)), " +
            "    'defendants', jsonb_agg(jsonb_build_object( " +
            "      'defendantId', def ->> 'id', " +
            "      'masterDefendantId', def ->> 'masterDefendantId' " +
            "    )) " +
            "  )) AS text) " +
            "  FROM prosecution_case p, " +
            "  LATERAL jsonb_array_elements(CAST(p.payload AS jsonb) -> 'defendants') AS def " +
            "  WHERE p.id IN (:caseIds) " +
            "  AND CAST(p.payload AS jsonb) -> 'migrationSourceSystem' ->> 'migrationCaseStatus' = 'INACTIVE' " +
            "  GROUP BY p.id", isNative = true)
    List<String> findInactiveMigratedCaseSummaries(@QueryParam("caseIds") List<UUID> caseIds);
}
