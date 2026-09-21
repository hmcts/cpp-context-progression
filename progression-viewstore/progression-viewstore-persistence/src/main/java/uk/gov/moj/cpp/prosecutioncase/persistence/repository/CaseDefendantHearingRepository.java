package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CaseDefendantHearingRepository
        extends JpaEntityRepository<CaseDefendantHearingEntity, CaseDefendantHearingKey> {

    private static final String CASE_ID = "caseId";
    private static final String HEARING_ID = "hearingId";
    private static final String DEFENDANT_ID = "defendantId";

    private static final String SELECT_ENTITY = "select entity from CaseDefendantHearingEntity entity";
    private static final String DELETE_ENTITY = "delete from CaseDefendantHearingEntity entity";

    private static final String WHERE_CASE_ID = " where entity.id.caseId in (:caseId)";
    private static final String WHERE_HEARING_ID = " where entity.id.hearingId in (:hearingId)";
    private static final String WHERE_DEFENDANT_ID = " where entity.id.defendantId in (:defendantId)";
    private static final String AND_CASE_ID = " and entity.id.caseId in (:caseId)";
    private static final String AND_DEFENDANT_ID = " and entity.id.defendantId in (:defendantId)";

    public CaseDefendantHearingRepository() {
        super(CaseDefendantHearingEntity.class);
    }

    @Override
    protected CaseDefendantHearingKey idOf(final CaseDefendantHearingEntity entity) {
        return entity.getId();
    }

    public List<CaseDefendantHearingEntity> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_CASE_ID,
                        CaseDefendantHearingEntity.class)
                .setParameter(CASE_ID, caseId)
                .getResultList();
    }

    public List<CaseDefendantHearingEntity> findByHearingId(final UUID hearingId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_HEARING_ID,
                        CaseDefendantHearingEntity.class)
                .setParameter(HEARING_ID, hearingId)
                .getResultList();
    }

    public List<CaseDefendantHearingEntity> findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_DEFENDANT_ID,
                        CaseDefendantHearingEntity.class)
                .setParameter(DEFENDANT_ID, defendantId)
                .getResultList();
    }

    public List<CaseDefendantHearingEntity> findByCaseIdAndDefendantId(final UUID caseId, final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_DEFENDANT_ID + AND_CASE_ID,
                        CaseDefendantHearingEntity.class)
                .setParameter(DEFENDANT_ID, defendantId)
                .setParameter(CASE_ID, caseId)
                .getResultList();
    }

    /**
     * Single result: throws NoResultException when absent, as the DeltaSpike @Query did by default.
     */
    public CaseDefendantHearingEntity findByHearingIdAndCaseIdAndDefendantId(final UUID hearingId,
                                                                            final UUID caseId,
                                                                            final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_HEARING_ID + AND_CASE_ID + AND_DEFENDANT_ID,
                        CaseDefendantHearingEntity.class)
                .setParameter(HEARING_ID, hearingId)
                .setParameter(CASE_ID, caseId)
                .setParameter(DEFENDANT_ID, defendantId)
                .getSingleResult();
    }

    public void removeByHearingIdAndCaseIdAndDefendantId(final UUID hearingId,
                                                         final UUID caseId,
                                                         final UUID defendantId) {
        entityManager.createQuery(DELETE_ENTITY + WHERE_HEARING_ID + AND_CASE_ID + AND_DEFENDANT_ID)
                .setParameter(HEARING_ID, hearingId)
                .setParameter(CASE_ID, caseId)
                .setParameter(DEFENDANT_ID, defendantId)
                .executeUpdate();
    }

    public void removeByHearingIdAndCaseId(final UUID hearingId, final UUID caseId) {
        entityManager.createQuery(DELETE_ENTITY + WHERE_HEARING_ID + AND_CASE_ID)
                .setParameter(HEARING_ID, hearingId)
                .setParameter(CASE_ID, caseId)
                .executeUpdate();
    }

    public void removeByHearingIdAndDefendantId(final UUID hearingId, final UUID defendantId) {
        entityManager.createQuery(DELETE_ENTITY + WHERE_HEARING_ID + AND_DEFENDANT_ID)
                .setParameter(HEARING_ID, hearingId)
                .setParameter(DEFENDANT_ID, defendantId)
                .executeUpdate();
    }

    public void removeByHearingId(final UUID hearingId) {
        entityManager.createQuery(DELETE_ENTITY + WHERE_HEARING_ID)
                .setParameter(HEARING_ID, hearingId)
                .executeUpdate();
    }
}
