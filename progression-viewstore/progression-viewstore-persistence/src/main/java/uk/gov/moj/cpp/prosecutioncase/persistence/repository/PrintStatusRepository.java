package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.PrintStatusType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrintStatus;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PrintStatusRepository extends EntityRepository<PrintStatus, UUID> {

    List<PrintStatus> findByStatus(final PrintStatusType resultOrderStatusType);

    List<PrintStatus> findByNotificationId(final UUID notificationId);

    List<PrintStatus> findByMaterialId(final UUID materialId);
}