package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface NotificationStatusRepository extends EntityRepository<NotificationStatusEntity, UUID> {

    List<NotificationStatusEntity> findByNotificationStatus(final NotificationStatus notificationStatus);

    List<NotificationStatusEntity> findByNotificationId(final UUID notificationId);

    List<NotificationStatusEntity> findByCaseId(final UUID caseId);

    List<NotificationStatusEntity> findByApplicationId(final UUID applicationId);
}