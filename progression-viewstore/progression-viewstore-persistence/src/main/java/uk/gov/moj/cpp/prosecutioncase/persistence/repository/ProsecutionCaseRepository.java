package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProsecutionCaseRepository extends JpaEntityRepository<ProsecutionCaseEntity, UUID> {

    public ProsecutionCaseRepository() {
        super(ProsecutionCaseEntity.class);
    }

    @Override
    protected UUID idOf(final ProsecutionCaseEntity entity) {
        return entity.getCaseId();
    }

    /**
     * Derived finder, so it throws NoResultException when absent - unlike
     * {@link #findOptionalByCaseId(UUID)} below, whose findOptionalBy prefix meant
     * SingleResultType.OPTIONAL and therefore null.
     */
    public ProsecutionCaseEntity findByCaseId(final UUID id) {
        return entityManager.createQuery(
                        "select e from ProsecutionCaseEntity e where e.caseId = :caseId",
                        ProsecutionCaseEntity.class)
                .setParameter("caseId", id)
                .getSingleResult();
    }

    public ProsecutionCaseEntity findOptionalByCaseId(final UUID id) {
        return entityManager.createQuery(
                        "select e from ProsecutionCaseEntity e where e.caseId = :caseId",
                        ProsecutionCaseEntity.class)
                .setParameter("caseId", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<ProsecutionCaseEntity> findByGroupId(final UUID groupId) {
        return entityManager.createQuery(
                        "select e from ProsecutionCaseEntity e where e.groupId = :groupId",
                        ProsecutionCaseEntity.class)
                .setParameter("groupId", groupId)
                .getResultList();
    }

    public List<ProsecutionCaseEntity> findByProsecutionCaseIds(final List<UUID> caseIds) {
        return entityManager.createQuery(
                        "select pce FROM ProsecutionCaseEntity pce where pce.caseId in (:caseIds)",
                        ProsecutionCaseEntity.class)
                .setParameter("caseIds", caseIds)
                .getResultList();
    }

    /**
     * Native PostgreSQL, kept verbatim: it builds a JSON summary with jsonb_build_object and
     * LATERAL jsonb_array_elements, neither of which has a JPQL equivalent. Returns the summaries as
     * text, one row per case.
     */
    @SuppressWarnings("unchecked")
    public List<String> findInactiveMigratedCaseSummaries(final List<UUID> caseIds) {
        return entityManager.createNativeQuery("""
                SELECT CAST(jsonb_build_object('inactiveCaseSummary', jsonb_build_object(
                    'id', p.id,
                    'caseURN', COALESCE(CAST(p.payload AS jsonb) -> 'prosecutionCaseIdentifier' -> 'caseURN', CAST('{}' AS jsonb)),
                    'migrationSourceSystem', COALESCE(CAST(p.payload AS jsonb) -> 'migrationSourceSystem', CAST('{}' AS jsonb)),
                    'defendants', jsonb_agg(jsonb_build_object(
                      'defendantId', def ->> 'id',
                      'masterDefendantId', def ->> 'masterDefendantId',
                      'personDefendant', COALESCE(CAST(def AS jsonb) -> 'personDefendant', CAST('{}' AS jsonb)),
                      'offences', COALESCE(CAST(def AS jsonb) -> 'offences', CAST('{}' AS jsonb))
                    ))
                  )) AS text)
                  FROM prosecution_case p,
                  LATERAL jsonb_array_elements(CAST(p.payload AS jsonb) -> 'defendants') AS def
                  WHERE p.id IN (:caseIds)
                  AND CAST(p.payload AS jsonb) -> 'migrationSourceSystem' ->> 'migrationCaseStatus' = 'INACTIVE'
                  GROUP BY p.id, p.payload
                """)
                .setParameter("caseIds", caseIds)
                .getResultList();
    }
}
