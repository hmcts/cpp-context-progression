package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationStatusRepository extends JpaEntityRepository<NotificationStatusEntity, UUID> {

    public NotificationStatusRepository() {
        super(NotificationStatusEntity.class);
    }

    @Override
    protected UUID idOf(final NotificationStatusEntity entity) {
        return entity.getId();
    }

    public List<NotificationStatusEntity> findByNotificationStatus(final NotificationStatus notificationStatus) {
        return entityManager.createQuery(
                        "select e from NotificationStatusEntity e where e.notificationStatus = :notificationStatus",
                        NotificationStatusEntity.class)
                .setParameter("notificationStatus", notificationStatus)
                .getResultList();
    }

    public List<NotificationStatusEntity> findByNotificationId(final UUID notificationId) {
        return entityManager.createQuery(
                        "select e from NotificationStatusEntity e where e.notificationId = :notificationId",
                        NotificationStatusEntity.class)
                .setParameter("notificationId", notificationId)
                .getResultList();
    }

    public List<NotificationStatusEntity> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select e from NotificationStatusEntity e where e.caseId = :caseId",
                        NotificationStatusEntity.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<NotificationStatusEntity> findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery(
                        "select e from NotificationStatusEntity e where e.applicationId = :applicationId",
                        NotificationStatusEntity.class)
                .setParameter("applicationId", applicationId)
                .getResultList();
    }
}
