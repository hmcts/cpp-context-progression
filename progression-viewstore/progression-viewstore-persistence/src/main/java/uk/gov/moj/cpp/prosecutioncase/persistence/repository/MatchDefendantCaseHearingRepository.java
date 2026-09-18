package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MatchDefendantCaseHearingRepository
        extends JpaEntityRepository<MatchDefendantCaseHearingEntity, UUID> {

    private static final String DEFENDANT_ID = "defendantId";
    private static final String HEARING_ID = "hearingId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";

    private static final String SELECT_ENTITY = "select e from MatchDefendantCaseHearingEntity e";

    public MatchDefendantCaseHearingRepository() {
        super(MatchDefendantCaseHearingEntity.class);
    }

    @Override
    protected UUID idOf(final MatchDefendantCaseHearingEntity entity) {
        return entity.getId();
    }

    public List<MatchDefendantCaseHearingEntity> findByMasterDefendantId(final UUID masterDefendantId) {
        return entityManager.createQuery(
                        SELECT_ENTITY
                                + " where e.masterDefendantId = :masterDefendantId",
                        MatchDefendantCaseHearingEntity.class)
                .setParameter(MASTER_DEFENDANT_ID, masterDefendantId)
                .getResultList();
    }

    public List<MatchDefendantCaseHearingEntity> findByMasterDefendantId(final List<UUID> masterDefendantId) {
        return entityManager.createQuery(
                        "select m from MatchDefendantCaseHearingEntity m"
                                + " where m.masterDefendantId in (:masterDefendantIds)",
                        MatchDefendantCaseHearingEntity.class)
                .setParameter("masterDefendantIds", masterDefendantId)
                .getResultList();
    }

    public List<MatchDefendantCaseHearingEntity> findByProsecutionCaseIdAndDefendantId(final UUID prosecutionCaseId,
                                                                                      final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_ENTITY
                                + " where e.prosecutionCaseId = :prosecutionCaseId and e.defendantId = :defendantId",
                        MatchDefendantCaseHearingEntity.class)
                .setParameter(PROSECUTION_CASE_ID, prosecutionCaseId)
                .setParameter(DEFENDANT_ID, defendantId)
                .getResultList();
    }

    public List<MatchDefendantCaseHearingEntity> findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery(
                        "select e from MatchDefendantCaseHearingEntity e where e.defendantId = :defendantId",
                        MatchDefendantCaseHearingEntity.class)
                .setParameter(DEFENDANT_ID, defendantId)
                .getResultList();
    }

    /**
     * Declared SingleResultType.OPTIONAL, so null rather than NoResultException when absent.
     */
    public MatchDefendantCaseHearingEntity findByHearingIdAndProsecutionCaseIdAndDefendantId(
            final UUID hearingId, final UUID prosecutionCaseId, final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_ENTITY
                                + " where e.hearingId = :hearingId"
                                + " and e.prosecutionCaseId = :prosecutionCaseId"
                                + " and e.defendantId = :defendantId",
                        MatchDefendantCaseHearingEntity.class)
                .setParameter(HEARING_ID, hearingId)
                .setParameter(PROSECUTION_CASE_ID, prosecutionCaseId)
                .setParameter(DEFENDANT_ID, defendantId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void removeByHearingIdAndCaseIdAndDefendantId(final UUID hearingId,
                                                         final UUID caseId,
                                                         final UUID defendantId) {
        entityManager.createQuery(
                        "delete from MatchDefendantCaseHearingEntity entity"
                                + " where entity.hearingId = :hearingId"
                                + " and entity.prosecutionCaseId = :caseId"
                                + " and entity.defendantId = :defendantId")
                .setParameter(HEARING_ID, hearingId)
                .setParameter("caseId", caseId)
                .setParameter(DEFENDANT_ID, defendantId)
                .executeUpdate();
    }

    public void removeByHearingId(final UUID hearingId) {
        entityManager.createQuery(
                        "delete from MatchDefendantCaseHearingEntity entity where entity.hearingId = :hearingId")
                .setParameter(HEARING_ID, hearingId)
                .executeUpdate();
    }
}
