package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HearingApplicationRepository
        extends JpaEntityRepository<HearingApplicationEntity, HearingApplicationKey> {

    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";

    private static final String SELECT_ENTITY = "select entity from HearingApplicationEntity entity";
    private static final String DELETE_ENTITY = "delete from HearingApplicationEntity entity";
    private static final String WHERE_HEARING_ID = " where entity.id.hearingId in (:hearingId)";
    private static final String WHERE_APPLICATION_ID = " where entity.id.applicationId in (:applicationId)";

    public HearingApplicationRepository() {
        super(HearingApplicationEntity.class);
    }

    @Override
    protected HearingApplicationKey idOf(final HearingApplicationEntity entity) {
        return entity.getId();
    }

    public List<HearingApplicationEntity> findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_APPLICATION_ID,
                        HearingApplicationEntity.class)
                .setParameter(APPLICATION_ID, applicationId)
                .getResultList();
    }

    public List<HearingApplicationEntity> findByHearingId(final UUID hearingId) {
        return entityManager.createQuery(
                        SELECT_ENTITY + WHERE_HEARING_ID,
                        HearingApplicationEntity.class)
                .setParameter(HEARING_ID, hearingId)
                .getResultList();
    }

    public void removeByHearingIdAndCourtApplicationId(final UUID hearingId, final UUID applicationId) {
        entityManager.createQuery(
                        DELETE_ENTITY + WHERE_HEARING_ID
                                + " and entity.id.applicationId in (:applicationId)")
                .setParameter(HEARING_ID, hearingId)
                .setParameter(APPLICATION_ID, applicationId)
                .executeUpdate();
    }

    public void removeByHearingId(final UUID hearingId) {
        entityManager.createQuery(DELETE_ENTITY + WHERE_HEARING_ID)
                .setParameter(HEARING_ID, hearingId)
                .executeUpdate();
    }

    public void removeByApplicationId(final UUID applicationId) {
        entityManager.createQuery(
                        DELETE_ENTITY + " where entity.id.applicationId = :applicationId")
                .setParameter(APPLICATION_ID, applicationId)
                .executeUpdate();
    }
}
